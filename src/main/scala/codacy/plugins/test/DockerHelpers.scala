package codacy.plugins.test

import java.nio.file.{Files, Path}

import codacy.TryExtension
import codacy.plugins.docker.Pattern
import com.codacy.analysis._
import com.codacy.plugins.api.languages.{Language, Languages}
import com.codacy.plugins.api.results.Tool
import com.codacy.plugins.results.traits.DockerTool
import com.codacy.plugins.utils.PluginHelper
import play.api.libs.json.Json

import _root_.scala.sys.process._
import scala.util.Try

object DockerHelpers {

  val dockerRunCmd = List("docker", "run", "--net=none", "--privileged=false", "--user=docker")

  def toPatterns(toolSpec: Tool.Specification): Seq[Pattern] = toolSpec.patterns.map {
    patternSpec =>
      val parameters = patternSpec.parameters.map(_.map { param =>
        (param.name.value, Json.toJson(param.default))
      }.toMap)

      Pattern(patternSpec.patternId.value, parameters)
  }.toSeq

  def toPatterns(patterns: Seq[PatternSimple]): Seq[Pattern] = patterns.map { pattern =>
    Pattern(pattern.name, pattern.parameters)
  }

  def testFoldersInDocker(dockerImageName: String): Seq[Path] = {
    val sourceDir = Files.createTempDirectory("docker-test-folders")

    val dockerStartedCmd = dockerRunCmd ++ List("-d", "--entrypoint=sh", dockerImageName)
    val output = dockerStartedCmd.lineStream_!

    val containerId = output.head
    //copy files from running container
    List("docker", "cp", s"$containerId:/docs/directory-tests", sourceDir.toString).lineStream_!.toList

    // backwards compatibility, making sure directory tests exist so we can copy the old test dir
    List("mkdir", "-p", s"$sourceDir/directory-tests").lineStream_!.toList
    List("docker", "cp", s"$containerId:/docs/tests", s"$sourceDir/directory-tests").lineStream_!.toList

    // Remove container
    List("docker", "rm", "-f", containerId).lineStream_!.toList
    val sourcesDir = sourceDir.resolve("directory-tests")

    sourcesDir.toFile.listFiles().collect {
      case dir if dir.exists() =>
        dir.toPath
    }
  }


  def readRawDoc(dockerImageName: String, name: String): Option[String] = {
    val cmd = dockerRunCmd ++ List("--rm=true", "--entrypoint=cat", dockerImageName, s"/docs/$name")
    Try(cmd.lineStream.toList).map(_.mkString(System.lineSeparator())).toOptionWithLog()
  }

  def withDocsDirectory[T](dockerImageName: String)(block: Path => T): T = {
    val sourceDir = Files.createTempDirectory("docker-docs-folders")
    val dockerStartedCmd = dockerRunCmd ++ List("-d", "--entrypoint=sh", dockerImageName)

    for {
      output <- Try(dockerStartedCmd.lineStream_!.toList).toOption
      containerId <- output.headOption
      // Copy files from running container
      _ <- Try(List("docker", "cp", s"$containerId:/docs", sourceDir.toString).lineStream_!.toList).toOption
      // Remove container
      _ <- Try(List("docker", "rm", "-f", containerId).lineStream_!.toList).toOption
    } yield ()

    val result = block(sourceDir.resolve("docs"))

    Try(List("rm", "-rf", sourceDir.toString).lineStream_!.toList)

    result
  }

  def findTools(testFiles: Seq[PatternTestFile], dockerImage: DockerImage): Seq[core.tools.Tool] = {
    val dockerImageName = dockerImage.name
    val dockerImageVersion = dockerImage.version
    (core.tools.Tool.availableTools ++ PluginHelper.dockerEnterprisePlugins).collectFirst {
      case tool if tool.dockerName == dockerImageName && tool.dockerTag == dockerImageVersion =>
        tool.languages.map {
          language => new core.tools.Tool(tool, language)
        }(collection.breakOut)
    }.getOrElse {
      val languages: Set[Language] =
        sys.props.get("codacy.tests.languages").map(_.split(",").flatMap(Languages.fromName).to[Set])
          .getOrElse {
            testFiles.flatMap(testFile => Languages.fromName(testFile.language.toString))(collection.breakOut)
          }
      val dockerTool = new DockerTool(dockerName = dockerImageName, true, languages,
        dockerImageName, dockerImageName, dockerImageName, "", "", "", needsCompilation = false,
        needsPatternsToRun = true, hasUIConfiguration = true) {
        override lazy val toolVersion: Option[String] = Some(dockerImageVersion)
        override val dockerTag: String = dockerImageVersion
      }
      languages.map {
        language => new core.tools.Tool(dockerTool, language)
      }(collection.breakOut)
    }
  }

}
