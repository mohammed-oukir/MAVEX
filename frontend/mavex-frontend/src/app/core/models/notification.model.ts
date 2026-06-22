export type NotificationLevel = 'INFO' | 'WARNING' | 'ERROR';

export interface NotificationItem {
  id:        number;
  title:     string;
  message:   string;
  level:     NotificationLevel;
  link:      string | null;
  read:      boolean;
  createdAt: string;
}
