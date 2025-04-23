# Print the current Java version
$javaVersion = java -version 2>&1
Write-Host "Current Java Version:"
Write-Host $javaVersion

# Define the root path to the collection of folders
$rootPath = "C:\path-to-folder\my-research-papers"

# Define the path to your jar file
$jarFilePath = "C:\path-to-jar\promptfx-x.x.x-jar-with-dependencies.jar"

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
    $command = "java -cp `"$jarFilePath`" tri.ai.cli.DocumentCliRunner --root=$rootPath --embedding=$embeddingModel --model=$model --temp=0.5 --max-tokens=2000 qa `"$question`""
    Write-Host $command

    # Execute the command and capture the output
    $output = Invoke-Expression $command

    # Append the question and the corresponding output to the output file
    Add-Content $outputFile "`nQuestion: $question"
    Add-Content $outputFile "Answer: $output"
    Add-Content $outputFile "`n---`n"
}

Write-Host "Processing complete. Results saved in $outputFile."

# Pause to allow the user to view the output before closing
Read-Host -Prompt "Press Enter to exit"