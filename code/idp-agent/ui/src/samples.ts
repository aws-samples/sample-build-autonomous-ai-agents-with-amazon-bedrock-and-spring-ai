// Bundled sample documents (real PDFs), one per expert persona. These power the
// one-click "Try a sample" buttons. The PDF files are served from the UI's public
// folder (public/samples) and mirror the files under code/idp-agent/samples/.

export interface SampleDoc {
  id: string;
  label: string;
  agentType: string;
  question: string;
  filename: string;
}

export const SAMPLES: SampleDoc[] = [
  {
    id: 'lease',
    label: 'Lawyer · Unsigned lease',
    agentType: 'LAWYER',
    question: 'Is this lease tenant-friendly, and is it signed?',
    filename: 'lease-agreement.pdf',
  },
  {
    id: 'mortgage',
    label: 'Mortgage · Loan application',
    agentType: 'MORTGAGE',
    question: 'Is this application complete for underwriting?',
    filename: 'mortgage-application.pdf',
  },
  {
    id: 'insurance',
    label: 'Insurance · Auto policy',
    agentType: 'INSURANCE',
    question: 'Is my coverage adequate and what is excluded?',
    filename: 'insurance-policy.pdf',
  },
  {
    id: 'tax',
    label: 'Tax · 1040 draft',
    agentType: 'TAX',
    question: 'Is this return ready to file?',
    filename: 'tax-return-summary.pdf',
  },
  {
    id: 'utility',
    label: 'General · Utility bill',
    agentType: 'GENERAL',
    question: 'Summarize this bill and tell me the due date and late fee.',
    filename: 'utility-bill.pdf',
  },
];

/** Fetch a bundled sample PDF and wrap it as a File for the normal upload path. */
export async function fetchSampleFile(sample: SampleDoc): Promise<File> {
  const res = await fetch(`/samples/${sample.filename}`);
  if (!res.ok) {
    throw new Error(`Could not load sample ${sample.filename} (${res.status})`);
  }
  const blob = await res.blob();
  return new File([blob], sample.filename, { type: 'application/pdf' });
}
