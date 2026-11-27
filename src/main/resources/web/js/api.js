function getSessionToken() {
  const meta = document.querySelector('meta[name="ppms-token"]');
  return meta ? meta.getAttribute("content") : "";
}

async function request(method, path, body = null) {
  const headers = {};
  const token = getSessionToken();
  if (token) {
    headers["X-PPMS-Token"] = token;
  }
  const opts = { method, headers, credentials: "same-origin" };
  if (body !== null) {
    headers["Content-Type"] = "application/json; charset=utf-8";
    opts.body = JSON.stringify(body);
  }

  let response;
  try {
    response = await fetch(path, opts);
  } catch (err) {
    throw new Error("Unable to reach local PPMS engine. Ensure the application is running.");
  }

  const text = await response.text();
  let data = {};
  if (text) {
    try {
      data = JSON.parse(text);
    } catch (_) {
      data = { error: text };
    }
  }

  if (!response.ok) {
    throw new Error(data.error || `Request failed (${response.status})`);
  }
  return data;
}

export const api = {
  get: path => request("GET", path),
  post: (path, body) => request("POST", path, body ?? {}),
  put: (path, body) => request("PUT", path, body ?? {}),
  del: path => request("DELETE", path),
  downloadUrl: path => {
    const a = document.createElement("a");
    a.href = path;
    a.download = "";
    document.body.appendChild(a);
    a.click();
    a.remove();
  }
};
