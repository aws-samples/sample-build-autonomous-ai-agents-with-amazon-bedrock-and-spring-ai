# Sample documents

One ready-to-use **PDF** per expert persona. Upload any of these in the UI (or `curl` them,
see below), pick the matching expert (or leave it on **Auto-detect**), and ask the
suggested question. The same PDFs power the one-click **"Try a sample"** buttons in the UI
(served from `../idp-agent-ui/public/samples/`). The curl examples below target the local
IDP backend on port 8081.

| PDF | Expert (agentType) | Suggested question | What to look for |
|---|---|---|---|
| `lease-agreement.pdf` | LAWYER | Is this lease tenant-friendly, and is it signed? | Unsigned-document **ALERT**, high-risk clauses (unlimited liability, entry without notice) |
| `mortgage-application.pdf` | MORTGAGE | Is this application complete for underwriting? | Missing-income **ALERT**, high debt-to-income **WARN** |
| `insurance-policy.pdf` | INSURANCE | Is my coverage adequate and what is excluded? | Coverage-gap **ALERT** (no UM/UIM or medical), expiring policy |
| `tax-return-summary.pdf` | TAX | Is this return ready to file? | Incomplete-filing **WARN** (missing totals and a 1099-DIV figure) |
| `utility-bill.pdf` | GENERAL | Summarize this bill and tell me the due date and late fee. | Clean structured summary; no alerts |

The agents are multimodal, so they read the PDF directly. The matching `.txt` files are the
editable sources used to generate the PDFs (`blog/build/gen-samples-pdf.mjs`); you can also
upload your own PDF, PNG, or JPG.

## Try them with curl

```bash
# Lawyer reviews an unsigned lease (PDF)
curl -X POST http://localhost:8081/api/documents \
  -F "agentType=LAWYER" \
  -F "question=Is this lease tenant-friendly, and is it signed?" \
  -F "file=@samples/lease-agreement.pdf;type=application/pdf"

# Mortgage advisor checks completeness
curl -X POST http://localhost:8081/api/documents \
  -F "agentType=MORTGAGE" \
  -F "file=@samples/mortgage-application.pdf;type=application/pdf"
```

Leave out `agentType` to let the classifier pick the right expert automatically.
