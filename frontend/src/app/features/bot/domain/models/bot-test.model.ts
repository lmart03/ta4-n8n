import { MessageSource } from './chat.model';

export type TestStatus = 'PENDING' | 'RUNNING' | 'PASSED' | 'FAILED' | 'MANUAL';

export interface TestCase {
  id: string;
  question: string | null;
  expected: string;
  status: TestStatus;
  reply: string | null;
  source: MessageSource | null;
  latencyMs: number | null;
  detail: string | null;
}

export interface TestRun {
  running: boolean;
  cases: TestCase[];
  startedAt: string | null;
}
