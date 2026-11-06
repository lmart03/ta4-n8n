import { explainFailure } from './failure-explanation.rule';

describe('explainFailure', () => {
  it('traduce NodeNotFoundException a una explicación comprensible y conserva el original', () => {
    const message = 'El robot se detuvo en «Clic: Seleccionar archivo» (NodeNotFoundException).';
    const explanation = explainFailure(message);

    expect(explanation.title).toBe('El robot no encontró un elemento de la página de SUNAT');
    expect(explanation.title).not.toContain('NodeNotFoundException');
    expect(explanation.technical).toBe(message);
  });

  it('reconoce el rechazo del firewall de SUNAT', () => {
    expect(explainFailure('The requested URL was rejected. Support ID: 123').title).toContain('SUNAT rechazó');
  });

  it('reconoce que el lanzador no pudo abrir el robot', () => {
    expect(explainFailure('El lanzador no pudo ejecutar el robot: sin uip').title).toBe('No se pudo abrir el robot en tu PC');
  });

  it('sin coincidencia da una explicación genérica y sin mensaje no inventa detalle técnico', () => {
    expect(explainFailure('algo raro').title).toBe('El flujo se detuvo antes de terminar');
    expect(explainFailure(null).technical).toBeNull();
    expect(explainFailure('   ').technical).toBeNull();
  });
});
