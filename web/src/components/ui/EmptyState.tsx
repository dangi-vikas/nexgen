import type { LucideIcon } from 'lucide-react';
import { Link } from 'react-router-dom';

interface EmptyStateProps {
  icon: LucideIcon;
  title: string;
  description: string;
  action?: { label: string; href?: string; onClick?: () => void };
}

export function EmptyState({ icon: Icon, title, description, action }: EmptyStateProps) {
  return (
    <div className="mx-auto flex max-w-md flex-col items-center py-16 text-center">
      <span className="grid size-14 place-items-center rounded-full bg-sage text-forest">
        <Icon className="size-6" strokeWidth={1.5} aria-hidden="true" />
      </span>
      <h2 className="mt-5 text-xl font-semibold tracking-[-0.03em]">{title}</h2>
      <p className="mt-2 text-sm leading-relaxed text-muted">{description}</p>
      {action ? (
        action.onClick ? (
          <button type="button" onClick={action.onClick} className="btn btn-primary mt-6">
            {action.label}
          </button>
        ) : action.href ? (
          <Link to={action.href} className="btn btn-primary mt-6">
            {action.label}
          </Link>
        ) : null
      ) : null}
    </div>
  );
}
