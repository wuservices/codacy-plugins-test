[![Codacy Badge](https://api.codacy.com/project/badge/grade/77e0473f417446a78758f02785a705b8)](https://www.codacy.com/app/Codacy/codacy-plugins-test)
[![Build Status](https://circleci.com/gh/codacy/codacy-plugins-test.svg?style=shield&circle-token=:circle-token)](https://circleci.com/gh/codacy/codacy-plugins-test)

# Codacy Plugins Test

Provide a testing interface for the external docker tools.

## Test definition

**Definition**

```javascript
//#Patterns: <PATTERN_NAME> : { "<PARAMETER_NAME>": "<PARAMETER_VALUE>" }


var people={};
//#<PATTERN_LEVEL>: <PATTERN_NAME>
for (var i = 0, person; person = people[i]; i++) {

}

var variable;
function test() {
//#<PATTERN_LEVEL>: <PATTERN_NAME>
    return variable = 'test';
}
```

**Example:**

```javascript
//#Patterns: boss


var people={};
//#Warn: boss
for (var i = 0, person; person = people[i]; i++) {

}

var variable;
function test() {
//#Warn: boss
    return variable = 'test';
}
```

## Usage

> PluginsTests

Checks if all the patterns have an occurrence in the test files

```sh
sbt "run-main codacy.plugins.DockerTest plugin codacy/jshint:1.0.3"
```

> PatternTests

Check if all the patterns defined in the test files occur in the specified line

```sh
sbt "run-main codacy.plugins.DockerTest pattern codacy/jshint:1.0.3"
sbt test
```

Alternatively, you can run a specific test file:

```sh
sbt "run-main codacy.plugins.DockerTest pattern codacy/jshint:1.0.3 no-curly-brackets"
sbt test
```

> All

```sh
sbt "run-main codacy.plugins.DockerTest all codacy/jshint:1.0.3"
```

## Docs

[Tool Developer Guide](http://docs.codacy.com/v1.5/docs/tool-developer-guide)

[Tool Developer Guide - Using Scala](http://docs.codacy.com/v1.5/docs/tool-developer-guide-using-scala)

## Troubleshooting

> OSx

Change the java tmp dir to your home so that boot2docker can access the tmp files

```sh
-Djava.io.tmpdir=$HOME/tmp
```
