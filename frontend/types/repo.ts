export interface Repo {
  id: number;
  fullName: string;          // "octocat/Hello-World"
  description: string | null;
  private: boolean;
  defaultBranch: string;
  language: string | null;
  sizeKb: number;
  updatedAt: string;
  indexStatus: IndexStatus;
}

export type IndexStatus =
  | "NOT_INDEXED"
  | "QUEUED"
  | "CLONING"
  | "CHUNKING"
  | "EMBEDDING"
  | "INDEXED"
  | "FAILED";

export interface Page<T> {
  items: T[];
  page: number;
  perPage: number;
  totalPages: number;
  totalItems: number;
}