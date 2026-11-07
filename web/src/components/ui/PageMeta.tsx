import { useEffect } from 'react';

interface PageMetaProps {
  title: string;
  description?: string;
}

export function PageMeta({ title, description }: PageMetaProps) {
  useEffect(() => {
    document.title = title;
    if (!description) return;
    const tag = document.querySelector('meta[name="description"]');
    if (tag) tag.setAttribute('content', description);
  }, [title, description]);

  return null;
}
