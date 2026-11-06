import { FlowStage, FlowStageKey, FlowStageState } from '../../domain/models/flow.model';
import { FlowStepperPresenter } from './flow-stepper.presenter';

const KEYS: FlowStageKey[] = ['excel', 'txt', 'sunat', 'zip', 'results'];

function stages(...states: FlowStageState[]): FlowStage[] {
  return KEYS.map((key, index) => ({
    key,
    title: key,
    description: key,
    state: states[index] ?? 'pending',
    detail: null,
    robotWorking: false,
  }));
}

describe('FlowStepperPresenter.plan', () => {
  it('si varias etapas cambian a la vez, se encienden una tras otra', () => {
    const presenter = new FlowStepperPresenter();
    const plan = presenter.plan(stages('done', 'done', 'active'));

    expect(plan.map((p) => p.circleMs)).toEqual([0, 650, 1300, 0, 0]);
    expect(plan[0].connectorMs).toBeGreaterThan(plan[0].circleMs);
  });

  it('una etapa que cambia sola se enciende al instante, sin esperas', () => {
    const presenter = new FlowStepperPresenter();
    presenter.plan(stages('done', 'done', 'active'));

    const plan = presenter.plan(stages('done', 'done', 'done', 'active'));
    expect(plan.map((p) => p.circleMs)).toEqual([0, 0, 0, 650, 0]);
  });

  it('sin cambios no hay retrasos', () => {
    const presenter = new FlowStepperPresenter();
    presenter.plan(stages('done', 'active'));
    expect(presenter.plan(stages('done', 'active')).every((p) => p.circleMs === 0)).toBe(true);
  });
});
