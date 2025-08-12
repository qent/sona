# Memory MCP Server Usage

You can persist long-term knowledge about this software project using the `memory` MCP server.

The server configuration:

```
"memory": {
  "command": "npx",
  "args": ["-y", "@modelcontextprotocol/server-memory"],
  "env": { "MEMORY_FILE_PATH": "/project_root/sona_memory.json" }
}
```

Before writing, verify that the file at `MEMORY_FILE_PATH` exists and create it if missing. This path points to `sona_memory.json` in the project root.

Use the tools exposed by this server to store and retrieve key details about the software-development project:
- architecture and design decisions;
- plans, milestones and outstanding tasks across planning, implementation, testing and release stages;
- important discussions, links and credentials (only after explicit user authorisation);
- lessons learned that will help future contributors.

Keep entries concise, relevant and free of unnecessary sensitive data.
