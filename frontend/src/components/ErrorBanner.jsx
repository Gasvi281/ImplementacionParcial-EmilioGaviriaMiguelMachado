export default function ErrorBanner({ error }) {
  if (!error) return null
  const fieldErrors = error.validationErrors ? Object.entries(error.validationErrors) : []
  return (
    <div className="error-banner">
      <div>{error.message}</div>
      {fieldErrors.length > 0 && (
        <ul style={{ margin: '6px 0 0', paddingLeft: 18 }}>
          {fieldErrors.map(([field, message]) => (
            <li key={field}>
              {field}: {message}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
