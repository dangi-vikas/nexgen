import { cn } from '@/lib/utils';

interface BotanicalProps {
  className?: string;
}

export function LeafSprig({ className }: BotanicalProps) {
  return (
    <svg
      viewBox="0 0 140 280"
      fill="none"
      className={cn('pointer-events-none', className)}
      aria-hidden="true"
    >
      <path
        d="M78 272C69 231 60 191 52 151C45 116 43 73 52 24"
        stroke="currentColor"
        strokeWidth="1.35"
        strokeLinecap="round"
      />
      <path
        d="M71 239C52 237 35 225 27 203C52 202 66 218 71 239Z"
        fill="currentColor"
        fillOpacity=".14"
        stroke="currentColor"
        strokeWidth="1.1"
        strokeLinejoin="round"
      />
      <path
        d="M67 214C76 191 92 180 112 181C109 204 89 215 67 214Z"
        fill="currentColor"
        fillOpacity=".14"
        stroke="currentColor"
        strokeWidth="1.1"
        strokeLinejoin="round"
      />
      <path
        d="M62 185C43 181 29 168 23 146C47 147 59 163 62 185Z"
        fill="currentColor"
        fillOpacity=".14"
        stroke="currentColor"
        strokeWidth="1.1"
        strokeLinejoin="round"
      />
      <path
        d="M58 159C68 137 84 126 104 127C101 150 81 161 58 159Z"
        fill="currentColor"
        fillOpacity=".14"
        stroke="currentColor"
        strokeWidth="1.1"
        strokeLinejoin="round"
      />
      <path
        d="M54 131C37 126 25 113 20 92C42 94 51 110 54 131Z"
        fill="currentColor"
        fillOpacity=".14"
        stroke="currentColor"
        strokeWidth="1.1"
        strokeLinejoin="round"
      />
      <path
        d="M51 105C60 85 75 75 94 76C91 97 73 107 51 105Z"
        fill="currentColor"
        fillOpacity=".14"
        stroke="currentColor"
        strokeWidth="1.1"
        strokeLinejoin="round"
      />
      <path
        d="M48 78C33 72 23 60 20 42C39 45 46 60 48 78Z"
        fill="currentColor"
        fillOpacity=".14"
        stroke="currentColor"
        strokeWidth="1.1"
        strokeLinejoin="round"
      />
      <path
        d="M48 53C56 37 68 29 84 30C81 47 65 56 48 53Z"
        fill="currentColor"
        fillOpacity=".14"
        stroke="currentColor"
        strokeWidth="1.1"
        strokeLinejoin="round"
      />
      <path
        d="M51 27C54 17 62 9 73 6C73 18 64 26 51 27Z"
        fill="currentColor"
        fillOpacity=".2"
        stroke="currentColor"
        strokeWidth="1.1"
        strokeLinejoin="round"
      />
    </svg>
  );
}

export function BotanicalFlower({ className }: BotanicalProps) {
  return (
    <svg
      viewBox="0 0 200 220"
      fill="none"
      className={cn('pointer-events-none', className)}
      aria-hidden="true"
    >
      <path
        d="M104 212C100 173 98 138 100 104"
        className="stroke-forest/50"
        strokeWidth="1.4"
        strokeLinecap="round"
      />
      <path
        d="M101 166C80 161 65 149 58 129C80 129 94 143 101 166Z"
        className="fill-forest/15 stroke-forest/50"
        strokeWidth="1.1"
        strokeLinejoin="round"
      />
      <path
        d="M102 142C116 123 133 114 152 114C148 135 130 145 102 142Z"
        className="fill-forest/15 stroke-forest/50"
        strokeWidth="1.1"
        strokeLinejoin="round"
      />
      <g className="fill-clay">
        {[0, 60, 120, 180, 240, 300].map((angle) => (
          <ellipse
            key={angle}
            cx="100"
            cy="34"
            rx="12.5"
            ry="27"
            transform={`rotate(${angle} 100 62)`}
          />
        ))}
      </g>
      <circle cx="100" cy="62" r="12" className="fill-clay-dark" />
      <circle cx="100" cy="62" r="4.5" className="fill-clay-soft" />
    </svg>
  );
}
