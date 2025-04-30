###
###
### sample-script2.ps1
###
### This script is designed to run a Java application that processes text files and generates answers to questions.
### The chunking process is executed first to enable control over chunking parameters (max-chunk-size).
### The Q&A process is executed second with command-line parameters for temperature (temp) and max # of tokens (max-tokens).
###
###

# Print the current Java version
$javaVersion = java -version 2>&1
Write-Host "Current Java Version:"
Write-Host $javaVersion

# Define the path to your jar file
$jarFilePath = "D:\code\aplpolaris\promptfx\promptkt\target\promptkt-0.10.3-SNAPSHOT-jar-with-dependencies.jar"

# Define the root path to the collection of folders
$rootPath = "D:\data\chatgpt\foundation-model-papers"

# Define the embedding model
$embeddingModel = "text-embedding-3-small"

# Generate the text chunks for the folder
$command1 = "java -cp `"$jarFilePath`" tri.ai.cli.DocumentCliRunner --root=$rootPath --embedding=$embeddingModel embeddings --reindex-new --max-chunk-size=1000"

Write-Host "----------------------------------------"
Write-Host "Generating text chunks for the folder completed..."
Write-Host $command1
Invoke-Expression $command1

# Print separator for the new task
Write-Host "."
Write-Host "."
Write-Host "."
Write-Host "----------------------------------------"
Write-Host "Generating answers for all questions..."

# Define the model
$model = "gpt-4o-mini"

# Define the file containing the list of questions
$questionsFile = "questions.txt"

# Define the output file to store the results
$outputFile = "outputs.txt"

# Define the output CSV file
$outputCsv = "results.csv"

# Clear the output file if it exists
if (Test-Path $outputCsv) {
    Remove-Item $outputCsv
}

# Clear the output file if it exists to start fresh
Clear-Content $outputFile

# Read the list of questions from the file
$questions = Get-Content $questionsFile

# Prepare an array to hold results
$results = @()

# Loop through each question
foreach ($question in $questions) {
    # Construct the command
    Write-Host $question
    $command2 = "java -cp `"$jarFilePath`" tri.ai.cli.DocumentCliRunner --root=$rootPath --embedding=$embeddingModel --model=$model --temp=0.5 --max-tokens=2000 qa --num-responses=2 `"$question`""
    Write-Host $command2

    # Execute the command and capture the output
    $outputLines = Invoke-Expression $command2

    # Append the question and the corresponding output to the output file
    Add-Content $outputFile "`nQuestion: $question"
    Add-Content $outputFile "Answer: $outputLines"
    Add-Content $outputFile "`n---`n"

    # Store the result as a custom object
    $answer = $outputLines -join "`n"
    $results += [PSCustomObject]@{
        Question = $question
        Answer   = $answer
    }
}

# Export the results to a CSV file
$results | Export-Csv -Path $outputCsv -NoTypeInformation -Encoding UTF8

Write-Host "Processing complete. Results saved in $outputFile and $outputCsv."

# Pause to allow the user to view the output before closing
Read-Host -Prompt "Press Enter to exit"