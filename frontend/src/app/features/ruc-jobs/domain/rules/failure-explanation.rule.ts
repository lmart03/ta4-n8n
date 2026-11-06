export interface FailureExplanation {
  /** Qué pasó, en palabras del usuario. */
  title: string;
  /** Qué puede hacer. */
  advice: string;
  /** El mensaje original (técnico), para el apartado expandible. */
  technical: string | null;
}

interface Pattern {
  test: RegExp;
  title: string;
  advice: string;
}

const PATTERNS: Pattern[] = [
  {
    test: /rechazad|rejected|support id/i,
    title: 'SUNAT rechazó el acceso del robot',
    advice:
      'El firewall de SUNAT bloquea el tráfico que le parece automático. Espera unos 10 minutos, cierra las ventanas de la consulta en Edge y vuelve a intentarlo.',
  },
  {
    test: /NodeNotFound|no se ha podido encontrar|elemento de la interfaz/i,
    title: 'El robot no encontró un elemento de la página de SUNAT',
    advice:
      'Suele pasar si la página no terminó de cargar, si SUNAT cambió algo o si la ventana de Edge estaba minimizada. Cierra las ventanas de la consulta en Edge y reintenta.',
  },
  {
    test: /lanzador no pudo|no se pudo ejecutar el robot|uip/i,
    title: 'No se pudo abrir el robot en tu PC',
    advice: 'Revisa que UiPath esté instalado y que el lanzador esté en marcha, y reintenta.',
  },
  {
    test: /timeout|tiempo de espera|tard[oó] demasiado/i,
    title: 'SUNAT tardó demasiado en responder',
    advice: 'Es un problema temporal de SUNAT. Espera un momento y reintenta.',
  },
  {
    test: /zip|resultado.*(inv[aá]lid|no se pudo)|archivo.*(inv[aá]lid|corrupt)/i,
    title: 'SUNAT devolvió un archivo que no se pudo leer',
    advice: 'Reintenta la consulta; si se repite, revisa el detalle técnico.',
  },
];

/**
 * Traduce el último mensaje de un fallo a una explicación comprensible. El mensaje original nunca se pierde:
 * queda en `technical` para mostrarlo en un apartado expandible. Función pura.
 */
export function explainFailure(message: string | null): FailureExplanation {
  const technical = message !== null && message.trim() !== '' ? message.trim() : null;
  const match = technical === null ? undefined : PATTERNS.find((pattern) => pattern.test.test(technical));
  if (match) return { title: match.title, advice: match.advice, technical };
  return {
    title: 'El flujo se detuvo antes de terminar',
    advice: 'Reintenta. Si vuelve a pasar, revisa el detalle técnico.',
    technical,
  };
}
