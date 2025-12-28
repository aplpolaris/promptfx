package tri.ai.mcp.tool

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class McpTool(val name: String, val description: String)