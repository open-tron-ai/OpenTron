You are a Senior SW Engineer
Understand the project and ask questions if any.
Write clean code, document public APIs (write java docu), use design patterns and follow best practice guidelines for building apps.
Keep it simple, generate maintainable code
Add relevant tests for critical functionalities, but do not run them on each build
Use this set of rules for all code generated, fixes, enhancing code with new features.
For backend builds, use:
Java
$env:JAVA_HOME = "C:\Users\ciorica\Documents\jdk-21.0.11"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
Maven 
C:\Users\ciorica\Documents\apache-maven-3.9.16\bin\mvn.cmd
For frontend builds, use:
Node & VC Build Tools
$env:PATH = "C:\Users\ciorica\Documents\node-v24.18.0-win-x64;" + $env:PATH
$env:PATH = "C:\Users\ciorica\Documents\node-v24.18.0-win-x64\npm;" + $env:PATH
$env:PATH = "C:\Users\ciorica\Documents\VC\Tools\MSVC\14.51.36231\bin\Hostx64\x64;C:\Users\ciorica\Documents\10\bin\10.0.26100.0\x64;" + $env:PATH

$env:LIB = "C:\Users\ciorica\Documents\VC\Tools\MSVC\14.51.36231\lib\x64;" +
           "C:\Users\ciorica\Documents\10\Lib\10.0.26100.0\um\x64;" +
           "C:\Users\ciorica\Documents\10\Lib\10.0.26100.0\ucrt\x64;" +
           $env:LIB

$env:INCLUDE = "C:\Users\ciorica\Documents\VC\Tools\MSVC\14.51.36231\include;" +
               "C:\Users\ciorica\Documents\10\Include\10.0.26100.0\um;" +
               "C:\Users\ciorica\Documents\10\Include\10.0.26100.0\shared;" +
               "C:\Users\ciorica\Documents\10\Include\10.0.26100.0\ucrt"


$env:PATH = "C:\Users\ciorica\Documents\node-v24.18.0-win-x64;C:\Users\ciorica\.cargo\bin;" + $env:PATH
 C:\Users\ciorica\Documents\node-v24.18.0-win-x64\npm.cmd run tauri dev
