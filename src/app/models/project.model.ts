export interface Project {
    id?: number;
    name: string;
    license: string;
    status: string;
    version: string;
    location: string;
  
    // Propiedades adicionales necesarias
    repositoryType: string;
    repository: string;
    odooVersion: string;
    subscriptionCode?: string;
    hostingLocation?: string;
  }