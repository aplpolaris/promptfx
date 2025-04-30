###
###
### sample-script.ps1
###
### This script is designed to run a Java application that processes text files and generates answers to questions.
###
###

# Print the current Java version
$javaVersion = java -version 2>&1
Write-Host "Current Java Version:"
Write-Host $javaVersion

# Define the root path to the collection of folders
$rootPath = "D:\data\chatgpt\foundation-model-papers"

# Define the path to your jar file
$jarFilePath = "D:\code\aplpolaris\promptfx\promptkt\target\promptkt-0.10.3-SNAPSHOT-jar-with-dependencies.jar"

# Define the model
$model = "gpt-4o-mini"

# Define the embedding model
$embeddingModel = "text-embedding-3-small"

# Define the file containing the list of questions
$questionsFile = "questions.txt"

# Define the output file to store the results
$outputFile = "outputs.txt"

# Clear the output file if it exists to start fresh
Clear-Content $outputFile

# Read the list of questions from the file
$questions = Get-Content $questionsFile

# Loop through each question
foreach ($question in $questions) {
    # Construct the command
    Write-Host $question
    $command = "java -cp `"$jarFilePath`" tri.ai.cli.DocumentCliRunner --root=$rootPath --embedding=$embeddingModel --model=$model --temp=0.5 --max-tokens=2000 qa --num-responses=2 `"$question`""
    Write-Host $command

    # Execute the command and capture the output
    $output = Invoke-Expression $command

    # Ensure output is a string with preserved newlines
    $outputText = if ($output -is [System.Array]) {
        $output -join "`n"
    } else {
        $output
    }

    # Append to file
    Add-Content $outputFile "`nQuestion: $question"
    Add-Content $outputFile "Answer:`n$outputText"
    Add-Content $outputFile "`n---`n"
}

Write-Host "Processing complete. Results saved in $outputFile."

# Pause to allow the user to view the output before closing
Read-Host -Prompt "Press Enter to exit"