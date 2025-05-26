export interface OdooInstance {
  id: number;
  name: string;
  category: string;
  url: string;
  containerName?: string;
  project?: {
    id: number;
    name: string;
  };
  
}
