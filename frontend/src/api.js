export class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.status = status;
  }
}
export function parseToken(response) {
  const token = String(response)
    .replace(/^JWT Token:\s*/i, "")
    .trim();
  if (!/^[\w-]+\.[\w-]+\.[\w-]+$/.test(token))
    throw new ApiError("The server returned an invalid login response.", 502);
  return token;
}
export function parseSymptoms(text) {
  const symptoms = [
    ...new Set(
      text
        .split(/[,\n]/)
        .map((s) => s.trim().replace(/\s+/g, " "))
        .filter(Boolean),
    ),
  ];
  if (symptoms.length > 20 || symptoms.some((s) => s.length > 200))
    throw new Error(
      "Enter up to 20 symptoms, each no longer than 200 characters.",
    );
  return symptoms;
}
export async function request(
  path,
  { token, body, method = "GET", signal } = {},
) {
  let response;
  const multipart = body instanceof FormData;
  try {
    response = await fetch(path, {
      method,
      signal,
      credentials: "omit",
      headers: {
        ...(token ? { Authorization: "Bearer " + token } : {}),
        ...(body !== undefined && !multipart
          ? { "Content-Type": "application/json" }
          : {}),
      },
      ...(body !== undefined
        ? { body: multipart ? body : JSON.stringify(body) }
        : {}),
    });
  } catch (error) {
    if (error.name === "AbortError") throw error;
    throw new ApiError(
      "Cannot reach the server. Check that Spring Boot is running on port 8080.",
      0,
    );
  }
  const raw = await response.text();
  let data;
  try {
    data = JSON.parse(raw);
  } catch {
    data = raw;
  }
  if (!response.ok) {
    if (response.status === 401 && token)
      window.dispatchEvent(new Event("session-expired"));
    const message =
      response.status === 401
        ? "Your login is invalid or expired. Please sign in again."
        : response.status === 403
          ? "This account does not have access. Check that you selected the correct role."
          : typeof data === "object" && data?.message
            ? data.message
            : "The request could not be completed. Please try again.";
    throw new ApiError(message, response.status);
  }
  return data;
}
