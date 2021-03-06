package codacy.plugins

import java.nio.file.Path

import codacy.plugins.test._
import codacy.utils.Printer
import org.apache.commons.io.FileUtils

case class Sources(mainSourcePath: Path, directoryPaths: Seq[Path])

object DockerTest {

  private lazy val config = Map("all" -> possibleTests) ++ possibleTests.map { test =>
    test.opt -> Seq(test)
  }
  private lazy val possibleTests = Seq(JsonTests, PluginsTests, PatternTests)
  private lazy val possibleTestNames = config.keySet

  def main(args: Array[String]): Unit = {
    val typeOfTests = args.headOption
    val dockerImageNameAndVersionOpt = args.drop(1).headOption
    val optArgs = args.drop(2)

    typeOfTests.fold(Printer.red(s"[Missing] test type -> [${possibleTestNames.mkString(", ")}]")) {
      case typeOfTest if possibleTestNames.contains(typeOfTest) =>
        dockerImageNameAndVersionOpt.fold(Printer.red("[Missing] docker ref -> dockerName:dockerVersion")) {
          dockerImageNameAndVersion =>
            val dockerImage = parseDockerImage(dockerImageNameAndVersion)

            val testSources = DockerHelpers.testFoldersInDocker(dockerImage)

            val allTestsPassed = possibleTests
              .map(test => run(testSources, test, typeOfTest, dockerImage, optArgs))
              .forall(identity)

            testSources.foreach(dir => FileUtils.deleteQuietly(dir.toFile))

            if (!allTestsPassed) {
              Printer.red("[Failure] Some tests failed!")
              System.exit(1)
            }

            Printer.green("[Success] All tests passed!")
        }
      case _ =>
    }
  }

  private def run(testSources: Seq[Path],
                  test: ITest,
                  testRequest: String,
                  dockerImage: DockerImage,
                  optArgs: Seq[String]): Boolean = {

    config.get(testRequest) match {
      case Some(ts) if ts.contains(test) =>
        test.run(testSources, dockerImage, optArgs) match {
          case true =>
            Printer.green(s"[Success] ${test.getClass.getSimpleName}")
            true
          case _ =>
            Printer.red(s"[Failure] ${test.getClass.getSimpleName}")
            false
        }
      case _ =>
        // this test was not selected
        true
    }
  }

  private def parseDockerImage(dockerImageNameAndVersion: String): DockerImage = {
    val (dockerImageName, dockerVersion) = dockerImageNameAndVersion.split(":") match {
      case Array(name, version) => (name, version)
      case Array(name) => (name, "latest")
      case _ => throw new RuntimeException("Invalid Docker Name.")
    }
    DockerImage(dockerImageName, dockerVersion)
  }
}
