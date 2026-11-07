import { useState, type FormEvent } from 'react';
import { ArrowRight, Eye, EyeOff, ShieldCheck } from 'lucide-react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { PageMeta } from '@/components/ui/PageMeta';
import { api } from '@/lib/api';
import { useUiStore } from '@/store/ui';

export function AuthPage() {
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const signIn = useUiStore((state) => state.signIn);
  const showToast = useUiStore((state) => state.showToast);
  const isRegister = pathname === '/register';

  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError('');
    setNotice('');

    if (!email.trim() || !password) {
      setError('Please enter your email and password.');
      return;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
      setError('Please enter a valid email address.');
      return;
    }
    if (isRegister && !name.trim()) {
      setError('Please tell us your name.');
      return;
    }
    if (isRegister && password !== confirm) {
      setError('Those passwords don’t match.');
      return;
    }
    if (password.length < 6) {
      setError('Use at least 6 characters for your password.');
      return;
    }

    setLoading(true);
    try {
      const user = isRegister
        ? await api.register(name.trim(), email.trim(), password)
        : await api.login(email.trim(), password);
      signIn(user);
      showToast({
        title: isRegister ? 'Welcome to NexGen' : 'Welcome back',
        detail: isRegister ? 'Your account is ready.' : `Signed in as ${user.email}`,
      });
      navigate('/account');
    } catch {
      setError('We couldn’t sign you in just now. Please try again.');
    } finally {
      setLoading(false);
    }
  }

  async function handleForgot() {
    setError('');
    setNotice('');
    const trimmed = email.trim();
    if (!trimmed) {
      setError('Enter your email address first and we’ll send a reset link.');
      return;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmed)) {
      setError('Please enter a valid email address.');
      return;
    }
    await api.requestPasswordReset(trimmed);
    setNotice('If that email is on file, a reset link is on its way.');
  }

  return (
    <>
      <PageMeta
        title={isRegister ? 'Create an account — NexGen Pharmacy' : 'Sign in — NexGen Pharmacy'}
        description="Sign in to manage prescriptions, orders and delivery details."
      />

      <div className="container-page py-8 sm:py-12">
        <div className="grid overflow-hidden rounded-[36px] border border-line bg-paper lg:grid-cols-[1fr_0.92fr]">
          <div className="flex flex-col justify-center px-6 py-10 sm:px-10 sm:py-14 lg:px-14">
            <div className="mx-auto w-full max-w-sm">
              <p className="eyebrow text-clay-dark">
                {isRegister ? 'Join NexGen' : 'Welcome back'}
              </p>
              <h1 className="display mt-3 text-[2.2rem] sm:text-[2.6rem]">
                {isRegister ? (
                  <>
                    Create your <span className="serif text-forest">account.</span>
                  </>
                ) : (
                  <>
                    Sign in to <span className="serif text-forest">NexGen.</span>
                  </>
                )}
              </h1>
              <p className="mt-3 text-[13.5px] leading-relaxed text-muted">
                {isRegister
                  ? 'Save prescriptions, track orders and book time with a pharmacist in a couple of taps.'
                  : 'Your prescriptions, orders and pharmacist notes, all in one place.'}
              </p>

              <form onSubmit={handleSubmit} className="mt-7 space-y-4" noValidate>
                {isRegister ? (
                  <div>
                    <label htmlFor="name" className="mb-1.5 block text-[11px] font-bold uppercase tracking-[0.12em] text-muted">
                      Full name
                    </label>
                    <input
                      id="name"
                      autoComplete="name"
                      value={name}
                      onChange={(event) => setName(event.target.value)}
                      className="field"
                      placeholder="Maya Ellison"
                    />
                  </div>
                ) : null}

                <div>
                  <label htmlFor="email" className="mb-1.5 block text-[11px] font-bold uppercase tracking-[0.12em] text-muted">
                    Email address
                  </label>
                  <input
                    id="email"
                    type="email"
                    autoComplete="email"
                    value={email}
                    onChange={(event) => setEmail(event.target.value)}
                    className="field"
                    placeholder="you@example.com"
                  />
                </div>

                <div>
                  <div className="mb-1.5 flex items-center justify-between">
                    <label htmlFor="password" className="text-[11px] font-bold uppercase tracking-[0.12em] text-muted">
                      Password
                    </label>
                    {!isRegister ? (
                      <button
                        type="button"
                        onClick={handleForgot}
                        className="text-[11px] font-semibold text-forest hover:text-clay-dark"
                      >
                        Forgot?
                      </button>
                    ) : null}
                  </div>
                  <div className="relative">
                    <input
                      id="password"
                      type={showPassword ? 'text' : 'password'}
                      autoComplete={isRegister ? 'new-password' : 'current-password'}
                      value={password}
                      onChange={(event) => setPassword(event.target.value)}
                      className="field pr-11"
                      placeholder="••••••••"
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword((value) => !value)}
                      className="absolute right-1.5 top-1/2 grid size-8 -translate-y-1/2 place-items-center rounded-full text-muted hover:bg-sage hover:text-ink"
                      aria-label={showPassword ? 'Hide password' : 'Show password'}
                      aria-pressed={showPassword}
                    >
                      {showPassword ? (
                        <EyeOff className="size-4" aria-hidden="true" />
                      ) : (
                        <Eye className="size-4" aria-hidden="true" />
                      )}
                    </button>
                  </div>
                </div>

                {isRegister ? (
                  <div>
                    <label htmlFor="confirm" className="mb-1.5 block text-[11px] font-bold uppercase tracking-[0.12em] text-muted">
                      Confirm password
                    </label>
                    <input
                      id="confirm"
                      type="password"
                      autoComplete="new-password"
                      value={confirm}
                      onChange={(event) => setConfirm(event.target.value)}
                      className="field"
                      placeholder="••••••••"
                    />
                  </div>
                ) : null}

                {isRegister ? (
                  <p className="text-[11px] leading-relaxed text-muted">
                    By creating an account you agree to our{' '}
                    <Link to="/help#terms" className="font-semibold text-forest underline underline-offset-2">
                      terms of sale
                    </Link>{' '}
                    and{' '}
                    <Link to="/help#privacy" className="font-semibold text-forest underline underline-offset-2">
                      privacy policy
                    </Link>
                    .
                  </p>
                ) : null}

                {error ? (
                  <p className="text-[12.5px] font-medium text-clay-dark" role="alert">
                    {error}
                  </p>
                ) : null}
                {notice ? (
                  <p className="text-[12.5px] font-medium text-forest" role="status">
                    {notice}
                  </p>
                ) : null}

                <button type="submit" disabled={loading} className="btn btn-primary w-full disabled:opacity-60">
                  {loading ? 'One moment…' : isRegister ? 'Create account' : 'Sign in'}
                  {!loading ? <ArrowRight className="size-4" aria-hidden="true" /> : null}
                </button>
              </form>

              <p className="mt-6 text-center text-[13px] text-muted">
                {isRegister ? 'Already have an account?' : 'New to NexGen?'}{' '}
                <Link
                  to={isRegister ? '/login' : '/register'}
                  className="font-bold text-forest hover:text-clay-dark"
                >
                  {isRegister ? 'Sign in' : 'Create an account'}
                </Link>
              </p>

              <p className="mt-8 flex items-center justify-center gap-1.5 text-[10.5px] text-muted">
                <ShieldCheck className="size-3.5" aria-hidden="true" />
                Your health information is encrypted and never sold.
              </p>
            </div>
          </div>

          <div className="relative hidden min-h-[620px] lg:block">
            <img
              src="/images/pharmacist-woman.jpg"
              alt="A NexGen pharmacist at the counter"
              width={1200}
              height={1800}
              loading="lazy"
              className="absolute inset-0 size-full object-cover object-[50%_35%]"
            />
            <div className="absolute inset-0 bg-gradient-to-t from-ink/80 via-ink/10 to-transparent" aria-hidden="true" />
            <div className="absolute inset-x-8 bottom-8 text-cream">
              <p className="serif text-[1.7rem] leading-tight">
                “Care is a conversation. The rest is just packaging.”
              </p>
              <p className="mt-3 text-[10px] font-bold uppercase tracking-[0.16em] text-cream/60">
                Amara Osei · Lead pharmacist
              </p>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
