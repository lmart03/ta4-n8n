import { Injectable, signal } from '@angular/core';

/** Lógica de vista de la zona de adjuntar: estado de arrastre, estilos y qué archivo se eligió. */
@Injectable()
export class FileUploadPresenter {
  readonly dragging = signal(false);

  zoneClasses(disabled: boolean): string {
    const base =
      'focus-within:ring-primary flex w-full cursor-pointer flex-col items-center justify-center gap-2 rounded-lg border border-dashed px-6 py-12 text-center transition-colors focus-within:ring-2 focus-within:ring-offset-2';
    if (disabled) return `${base} border-border bg-background cursor-not-allowed opacity-60`;
    return this.dragging()
      ? `${base} border-primary bg-primary-soft`
      : `${base} border-neutral-300 bg-surface hover:border-primary hover:bg-primary-soft/50`;
  }

  onDragOver(event: DragEvent, disabled: boolean): void {
    event.preventDefault();
    if (!disabled) this.dragging.set(true);
  }

  onDragLeave(): void {
    this.dragging.set(false);
  }

  /** Devuelve el archivo soltado, o `null` si no hay o la zona está deshabilitada. */
  onDrop(event: DragEvent, disabled: boolean): File | null {
    event.preventDefault();
    this.dragging.set(false);
    const file = event.dataTransfer?.files?.[0] ?? null;
    return file && !disabled ? file : null;
  }

  /** Devuelve el archivo elegido con el explorador y deja el input listo para elegir el mismo otra vez. */
  onPicked(event: Event): File | null {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    // Permite volver a elegir el mismo archivo después de quitarlo.
    input.value = '';
    return file;
  }
}
