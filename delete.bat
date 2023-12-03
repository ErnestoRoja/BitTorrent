@echo off
for /r %%i in (*.class) do (
    echo Deleting: %%i
    del "%%i"
)

javac peerProcess.java