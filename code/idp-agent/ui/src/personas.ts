// Presentation metadata for the specialist personas. The backend exposes the raw agent
// names via GET /api/agents (LAWYER, MORTGAGE, INSURANCE, TAX, GENERAL); this module
// gives each one an icon, a friendly name, a short description, and the kinds of fields
// the specialist typically extracts, so the picker can render rich, informative cards.

export interface Persona {
  /** Raw agent type as returned by the backend. */
  type: string;
  icon: string;
  name: string;
  tagline: string;
  /** Examples of what this specialist pulls out of a document. */
  extracts: string[];
}

export const PERSONAS: Record<string, Persona> = {
  LAWYER: {
    type: 'LAWYER',
    icon: '\u2696\uFE0F', // scales of justice
    name: 'Lawyer',
    tagline: 'Contracts, leases, NDAs and legal agreements.',
    extracts: ['Parties', 'Term & dates', 'Obligations', 'Signatures', 'Risk clauses'],
  },
  MORTGAGE: {
    type: 'MORTGAGE',
    icon: '\uD83C\uDFE0', // house
    name: 'Mortgage advisor',
    tagline: 'Loan applications and underwriting packets.',
    extracts: ['Applicant', 'Loan amount', 'Income', 'Property', 'Completeness'],
  },
  INSURANCE: {
    type: 'INSURANCE',
    icon: '\uD83D\uDEE1\uFE0F', // shield
    name: 'Insurance advisor',
    tagline: 'Policies, coverage summaries and claims.',
    extracts: ['Policy number', 'Coverage', 'Limits', 'Exclusions', 'Premium'],
  },
  TAX: {
    type: 'TAX',
    icon: '\uD83D\uDCCA', // bar chart
    name: 'Tax advisor',
    tagline: 'Tax returns, W-2s and financial forms.',
    extracts: ['Filer', 'Tax year', 'Income', 'Deductions', 'Balance due'],
  },
  GENERAL: {
    type: 'GENERAL',
    icon: '\uD83D\uDCC4', // page
    name: 'General assistant',
    tagline: 'Bills, letters and anything else.',
    extracts: ['Key fields', 'Dates', 'Amounts', 'Summary'],
  },
};

/** Persona metadata for an agent type, with a safe fallback for unknown values. */
export function personaFor(type: string): Persona {
  return (
    PERSONAS[type] ?? {
      type,
      icon: '\uD83D\uDCC4',
      name: type,
      tagline: 'Specialist document reviewer.',
      extracts: [],
    }
  );
}
