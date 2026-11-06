import { Chat, ChatMessage, MessageSource } from '../models/chat.model';

const SOURCE_LABELS: Record<MessageSource, string> = {
  FAQ: 'FAQ',
  IA: 'Gemini',
  CONTINGENCIA: 'Contingencia',
  DERIVADA: 'Derivada a recepción',
  RECHAZO: 'Rechazada',
  AYUDA: 'Ayuda',
  HUMANO: 'Recepción',
};

export function sourceLabel(source: MessageSource | null): string {
  return source === null ? '' : SOURCE_LABELS[source];
}

/** Iniciales para el avatar: primera letra de las dos primeras palabras del nombre. */
export function initials(name: string): string {
  const letters = name
    .trim()
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((word) => word[0].toUpperCase());
  return letters.length > 0 ? letters.join('') : '?';
}

const startOfDay = (date: Date): number => new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime();

/** Hora del chat en la lista: "14:32" si es de hoy, "ayer" o "06/11" si es más viejo. */
export function chatTime(iso: string, now: number): string {
  const date = new Date(iso);
  const days = Math.round((startOfDay(new Date(now)) - startOfDay(date)) / 86_400_000);
  if (days <= 0) return date.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit', hour12: false });
  if (days === 1) return 'ayer';
  return date.toLocaleDateString('es-PE', { day: '2-digit', month: '2-digit' });
}

/** Título del separador entre días dentro de la conversación. */
export function dayLabel(iso: string, now: number): string {
  const date = new Date(iso);
  const days = Math.round((startOfDay(new Date(now)) - startOfDay(date)) / 86_400_000);
  if (days <= 0) return 'Hoy';
  if (days === 1) return 'Ayer';
  return date.toLocaleDateString('es-PE', { weekday: 'long', day: 'numeric', month: 'long' });
}

export function messageTime(iso: string): string {
  return new Date(iso).toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit', hour12: false });
}

/** Inserta o actualiza un chat y deja la lista con el más reciente primero. */
export function upsertChat(chats: Chat[], chat: Chat): Chat[] {
  const others = chats.filter((item) => item.id !== chat.id);
  return [chat, ...others].sort((a, b) => b.lastMessageAt.localeCompare(a.lastMessageAt));
}

/** Agrega un mensaje a una conversación ordenada de más viejo a más nuevo, sin duplicar el mismo id. */
export function appendMessage(messages: ChatMessage[], message: ChatMessage): ChatMessage[] {
  if (messages.some((item) => item.id === message.id)) return messages;
  return [...messages, message].sort((a, b) => a.createdAt.localeCompare(b.createdAt));
}

/** Filtra por nombre, usuario o texto del último mensaje (sin distinguir mayúsculas ni tildes). */
export function filterChats(chats: Chat[], text: string): Chat[] {
  const needle = fold(text);
  if (needle === '') return chats;
  return chats.filter((chat) => fold(`${chat.name} ${chat.username ?? ''} ${chat.lastText}`).includes(needle));
}

function fold(value: string): string {
  return value
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .toLowerCase()
    .trim();
}
