/** The drawing that stands in for a wish without a picture: a turquoise box with a red ribbon. */
export function GiftIcon({ className = "gift-icon" }: { className?: string }) {
  return (
    <svg className={className} viewBox="80 76 352 352" aria-hidden="true" focusable="false">
      <rect x="126" y="268" width="260" height="150" rx="10" fill="#ffffff" stroke="#0e7c80" strokeWidth="14" />
      <rect x="106" y="196" width="300" height="72" rx="12" fill="#3cc9bd" stroke="#0e7c80" strokeWidth="14" />
      <rect x="236" y="196" width="40" height="222" fill="#d62839" />
      <path d="M256 196 C206 50 118 60 150 180 C168 214 232 204 256 196 Z" fill="#d62839" />
      <path d="M256 196 C306 50 394 60 362 180 C344 214 280 204 256 196 Z" fill="#d62839" />
      <circle cx="256" cy="194" r="24" fill="#a81c2b" />
    </svg>
  );
}
