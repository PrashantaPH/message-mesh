/** Small, dependency-free unique id for optimistic message reconciliation. */
export function nanoidLike(): string {
  return (
    Date.now().toString(36) +
    Math.random().toString(36).slice(2, 10)
  );
}
