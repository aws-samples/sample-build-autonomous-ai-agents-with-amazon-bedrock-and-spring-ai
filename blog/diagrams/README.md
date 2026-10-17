# Diagrams

| Diagram | Shows | Files |
|---|---|---|
| Deployment architecture | Everything deployed on AWS: the AgentCore Runtime path, the ECS web path, the managed AI services, ECR, IAM, and CloudWatch | [SVG](deployment-architecture.svg) · [PNG](deployment-architecture.png) |
| Spring AI on AgentCore Runtime | One request through AgentCore Runtime into the Spring AI agent container (runtime starter, `@AgentCoreInvocation`, ChatClient, tools, Bedrock) | [SVG](springai-agentcore-overview.svg) · [PNG](springai-agentcore-overview.png) |

The SVG files are the source. The PNGs are 2× renders (2800 px wide) for Word, slides,
and the blog CMS. To re-render after editing an SVG, run this from this folder (Node.js 20+):

```bash
npm install --no-save @resvg/resvg-js@2.6.2
node -e "const {Resvg}=require('@resvg/resvg-js'),fs=require('fs');for(const n of ['deployment-architecture','springai-agentcore-overview']){const p=new Resvg(fs.readFileSync(n+'.svg','utf8'),{fitTo:{mode:'zoom',value:2},background:'#ffffff',font:{loadSystemFonts:true,defaultFontFamily:'Arial'}}).render();fs.writeFileSync(n+'.png',p.asPng());console.log(n+'.png',p.width+'x'+p.height)}"
```

The plain-language walkthrough is in
[../architecture-and-agentcore-runtime.md](../architecture-and-agentcore-runtime.md).
