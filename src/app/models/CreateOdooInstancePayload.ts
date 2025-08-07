export interface CreateOdooInstancePayload {
  name: string;
  category: string;
  projectId: number;
  neutralize?: boolean;
  codeSourceCategory?: string | null;
  copyDataFromProduction?: boolean;
}
