interface PlaceholderPageProps {
  title: string;
}

export default function PlaceholderPage({ title }: PlaceholderPageProps) {
  return (
    <div className="page">
      <h1 className="page-title">{title}</h1>
      <div className="card card--padded mt-6 flex flex-col items-center justify-center py-16 text-center">
        <div className="mb-3 flex h-12 w-12 items-center justify-center rounded-full bg-brand-soft text-xl text-brand">
          ✦
        </div>
        <h2 className="text-base font-semibold text-slate-900">
          Coming soon
        </h2>
        <p className="mt-1 max-w-sm text-sm text-slate-500">
          This area is part of a future milestone. Check back once it's ready.
        </p>
      </div>
    </div>
  );
}
