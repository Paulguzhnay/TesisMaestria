export interface OdooInstance {
    id?: number;
    name: string;
    url: string;
    license: string;
    status: string;
    version: string;
    category: 'DEVELOPMENT' | 'STAGING' | 'PRODUCTION';
  }
  