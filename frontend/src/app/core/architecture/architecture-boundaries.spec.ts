import { existsSync, readFileSync, readdirSync, statSync } from 'fs';
import { join } from 'path';

const appRoot = join(__dirname, '..', '..');

function filesUnder(directory: string): string[] {
  return readdirSync(directory).flatMap((name) => {
    const path = join(directory, name);
    return statSync(path).isDirectory() ? filesUnder(path) : [path];
  });
}

const normalize = (path: string): string => path.replace(/\\/g, '/');

describe('límites de arquitectura Angular', () => {
  it('impide que la UI importe servicios API de infraestructura (debe pasar por la fachada)', () => {
    const uiFiles = filesUnder(appRoot).filter((path) => {
      return path.endsWith('.ts') && /\/features\/.+\/ui\//.test(normalize(path)) && !path.endsWith('.spec.ts');
    });
    const violations = uiFiles.filter((path) =>
      /from\s+['"][^'"]*infrastructure\/api\//.test(readFileSync(path, 'utf8')),
    );
    expect(violations).toEqual([]);
  });

  it('mantiene la capa de dominio pura: sin dependencias de Angular ni de la UI', () => {
    const domainFiles = filesUnder(appRoot).filter((path) => {
      return path.endsWith('.ts') && !path.endsWith('.spec.ts') && /\/features\/.+\/domain\//.test(normalize(path));
    });
    const violations = domainFiles.filter((path) =>
      /from\s+['"](@angular|src\/app\/ui|src\/app\/shared)/.test(readFileSync(path, 'utf8')),
    );
    expect(violations).toEqual([]);
  });

  it('impide que core y shared dependan de las features', () => {
    const files = ['core', 'shared']
      .flatMap((dir) => filesUnder(join(appRoot, dir)))
      .filter((path) => path.endsWith('.ts') && !path.endsWith('.spec.ts'));
    const violations = files.filter((path) =>
      /from\s+['"](src\/app\/features|@features)\//.test(readFileSync(path, 'utf8')),
    );
    expect(violations).toEqual([]);
  });

  it('cada componente tiene su presenter y su plantilla en un .html aparte', () => {
    const components = filesUnder(appRoot).filter((path) => path.endsWith('.component.ts') && !path.endsWith('.spec.ts'));
    expect(components.length).toBeGreaterThan(0);
    const problems = components.flatMap((path) => {
      const base = path.replace(/\.component\.ts$/, '');
      const missing: string[] = [];
      if (!existsSync(`${base}.presenter.ts`)) missing.push(`${base}.presenter.ts`);
      if (!existsSync(`${base}.component.html`)) missing.push(`${base}.component.html`);
      if (/\btemplate\s*:/.test(readFileSync(path, 'utf8'))) missing.push(`${path} (plantilla en línea)`);
      return missing.map((item) => item.replace(appRoot, ''));
    });
    expect(problems).toEqual([]);
  });

  it('las acciones y los efectos de NgRx solo viven en application/store', () => {
    const offenders = filesUnder(appRoot)
      .filter((path) => path.endsWith('.ts') && !path.endsWith('.spec.ts'))
      .filter((path) => /\/(ui|domain)\//.test(normalize(path)))
      .filter((path) => /from\s+['"]@ngrx\/(effects|entity)/.test(readFileSync(path, 'utf8')))
      .map((path) => path.replace(appRoot, ''));
    expect(offenders).toEqual([]);
  });

  it('mantiene tipada la capa genérica: sin `any` explícito en core, shared y ui', () => {
    const files = ['core', 'shared', 'ui']
      .flatMap((dir) => filesUnder(join(appRoot, dir)))
      .filter((path) => /\.(ts|html)$/.test(path) && !path.endsWith('.spec.ts'));
    const anyUsage = /:\s*any\b|\bas\s+any\b|<any[>,\s[]|\bany\[\]|\$any\(/;
    const violations = files
      .filter((path) =>
        readFileSync(path, 'utf8')
          .split('\n')
          .some((line) => anyUsage.test(line.replace(/\/\/.*$/, ''))),
      )
      .map((path) => path.replace(appRoot, ''));
    expect(violations).toEqual([]);
  });
});
