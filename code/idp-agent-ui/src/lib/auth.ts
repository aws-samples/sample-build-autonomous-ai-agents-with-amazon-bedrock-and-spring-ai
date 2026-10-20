// Lightweight client-side session for the demo UI. In production, replace this with
// an OAuth2 / Amazon Cognito flow and a verified JWT (the backend already supports an
// IAM/JWT authorizer on AgentCore Runtime).

export interface User {
  name: string;
  loginAt: number;
}

const KEY = 'idp.user';

export function getUser(): User | null {
  try {
    const s = localStorage.getItem(KEY);
    return s ? (JSON.parse(s) as User) : null;
  } catch {
    return null;
  }
}

export function login(name: string): User {
  const user: User = { name: name.trim(), loginAt: Date.now() };
  localStorage.setItem(KEY, JSON.stringify(user));
  return user;
}

export function logout(): void {
  localStorage.removeItem(KEY);
}
