import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../AuthContext";
import { getApiErrorMessage } from "../../../shared/api/errors";

const ROLE_OPTIONS = ["PATIENT", "DOCTOR"];

export function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();

  const [form, setForm] = useState({
    role: "PATIENT",
    fullName: "",
    email: "",
    password: ""
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setSuccess("");
    setLoading(true);

    try {
      await register(form);
      setSuccess("Account created successfully. You can now sign in.");
      setTimeout(() => navigate("/login"), 900);
    } catch (submitError) {
      setError(getApiErrorMessage(submitError, "Could not create account"));
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="mx-auto max-w-xl rounded-2xl border border-white/90 bg-white/80 p-8 shadow-soft">
      <h1 className="text-3xl font-extrabold text-brand-700">Create account</h1>
      <p className="mt-2 text-sm text-slate-600">Register as patient or doctor.</p>

      <form className="mt-6 space-y-4" onSubmit={handleSubmit}>
        <div>
          <p className="mb-2 text-sm font-semibold text-slate-700">Account type</p>
          <div className="flex gap-2">
            {ROLE_OPTIONS.map((role) => (
              <button
                key={role}
                type="button"
                onClick={() => setForm((prev) => ({ ...prev, role }))}
                className={`rounded-lg px-4 py-2 text-sm font-semibold transition ${
                  form.role === role
                    ? "bg-brand-600 text-white"
                    : "border border-slate-300 text-slate-700 hover:bg-brand-100"
                }`}
              >
                {role}
              </button>
            ))}
          </div>
        </div>

        <label className="block">
          <span className="mb-1 block text-sm font-semibold text-slate-700">Full name</span>
          <input
            type="text"
            required
            value={form.fullName}
            onChange={(event) => setForm((prev) => ({ ...prev, fullName: event.target.value }))}
            className="w-full rounded-lg border border-slate-300 px-3 py-2 outline-none transition focus:border-brand-600"
          />
        </label>

        <label className="block">
          <span className="mb-1 block text-sm font-semibold text-slate-700">Email</span>
          <input
            type="email"
            required
            value={form.email}
            onChange={(event) => setForm((prev) => ({ ...prev, email: event.target.value }))}
            className="w-full rounded-lg border border-slate-300 px-3 py-2 outline-none transition focus:border-brand-600"
          />
        </label>

        <label className="block">
          <span className="mb-1 block text-sm font-semibold text-slate-700">Password</span>
          <input
            type="password"
            required
            minLength={8}
            value={form.password}
            onChange={(event) => setForm((prev) => ({ ...prev, password: event.target.value }))}
            className="w-full rounded-lg border border-slate-300 px-3 py-2 outline-none transition focus:border-brand-600"
          />
        </label>

        {error && <p className="rounded-lg bg-rose-100 px-3 py-2 text-sm text-rose-700">{error}</p>}
        {success && <p className="rounded-lg bg-emerald-100 px-3 py-2 text-sm text-emerald-700">{success}</p>}

        <button
          type="submit"
          disabled={loading}
          className="w-full rounded-lg bg-brand-600 px-4 py-2 font-semibold text-white transition hover:bg-brand-700 disabled:cursor-not-allowed disabled:opacity-70"
        >
          {loading ? "Creating account..." : "Create account"}
        </button>
      </form>

      <p className="mt-6 text-sm text-slate-600">
        Already registered?{" "}
        <Link to="/login" className="font-semibold text-brand-700 underline-offset-4 hover:underline">
          Go to login
        </Link>
      </p>
    </section>
  );
}
