import { useEffect, useState } from 'react';
import { ArrowDown, ArrowUpRight, ChefHat, Heart, LogOut, Plus, Soup, Users, X } from 'lucide-react';

const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api/food-posts';
const API_BASE_URL = API_URL.replace(/\/food-posts\/?$/, '');
const AUTH_URL = `${API_BASE_URL}/auth`;

function readSession() {
  try {
    return JSON.parse(sessionStorage.getItem('foodshare-session') ?? 'null');
  } catch {
    return null;
  }
}

async function responseMessage(response, fallback) {
  try {
    const body = await response.json();
    return body.detail || body.message || fallback;
  } catch {
    return fallback;
  }
}

function App() {
  const [session, setSession] = useState(readSession);
  const [authReady, setAuthReady] = useState(false);
  const [posts, setPosts] = useState([]);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [authError, setAuthError] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  const [isAuthenticating, setIsAuthenticating] = useState(false);
  const [isRegistering, setIsRegistering] = useState(false);

  async function loadPosts() {
    setError('');
    try {
      const response = await fetch(API_URL);
      if (!response.ok) throw new Error('Could not load the community table.');
      setPosts(await response.json());
    } catch {
      setError('We couldn’t reach the kitchen. Check that the API is running, then try again.');
    } finally {
      setIsLoading(false);
    }
  }

  useEffect(() => {
    loadPosts();
    if (!session?.token) {
      setAuthReady(true);
      return;
    }

    fetch(`${AUTH_URL}/me`, { headers: { Authorization: `Bearer ${session.token}` } })
      .then(async (response) => {
        if (!response.ok) throw new Error('Your session has expired. Please sign in again.');
        const user = await response.json();
        const currentSession = { ...session, user };
        sessionStorage.setItem('foodshare-session', JSON.stringify(currentSession));
        setSession(currentSession);
      })
      .catch(() => {
        sessionStorage.removeItem('foodshare-session');
        setSession(null);
      })
      .finally(() => setAuthReady(true));
  }, []);

  async function handleAuthentication(event) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const payload = {
      email: form.get('email'),
      password: form.get('password'),
    };
    if (isRegistering) payload.displayName = form.get('displayName');

    setIsAuthenticating(true);
    setAuthError('');
    try {
      const response = await fetch(`${AUTH_URL}/${isRegistering ? 'register' : 'login'}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });
      if (!response.ok) {
        throw new Error(await responseMessage(response,
          isRegistering ? 'We couldn’t create your account.' : 'We couldn’t sign you in.'));
      }
      const authenticatedSession = await response.json();
      sessionStorage.setItem('foodshare-session', JSON.stringify(authenticatedSession));
      setSession(authenticatedSession);
      setAuthError('');
    } catch (authenticationError) {
      setAuthError(authenticationError.message || 'Please try again.');
    } finally {
      setIsAuthenticating(false);
    }
  }

  async function handleLogout() {
    if (session?.token) {
      try {
        await fetch(`${AUTH_URL}/logout`, {
          method: 'POST',
          headers: { Authorization: `Bearer ${session.token}` },
        });
      } catch {
        setError('Could not contact the server to end your session. Your local session was cleared.');
      }
    }
    sessionStorage.removeItem('foodshare-session');
    setSession(null);
    setAuthError('');
  }

  async function handleSubmit(event) {
    event.preventDefault();
    const formElement = event.currentTarget;
    const form = new FormData(formElement);
    setIsSaving(true);
    setError('');
    try {
      const response = await fetch(API_URL, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${session.token}`,
        },
        body: JSON.stringify({
          foodName: form.get('foodName'),
          description: form.get('description'),
          servings: Number(form.get('servings')),
        }),
      });
      if (response.status === 401) {
        sessionStorage.removeItem('foodshare-session');
        setSession(null);
        throw new Error('Your session has expired. Please sign in again before sharing food.');
      }
      if (!response.ok) {
        throw new Error(await responseMessage(response, 'Your post couldn’t be shared. Please try again.'));
      }
      const newPost = await response.json();
      setPosts((current) => [newPost, ...current]);
      formElement.reset();
      setIsFormOpen(false);
    } catch (submitError) {
      setError(submitError.message || 'Your post couldn’t be shared. Please try again.');
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <main>
      <header className="topbar">
        <a className="brand" href="#" aria-label="Good Neighbour home">
          <span className="brand-mark"><Soup size={20} strokeWidth={2.2} /></span>
          <span>good neighbour<span className="brand-dot">.</span></span>
        </a>
        <div className="topbar-actions">
          <a className="top-link" href="#community">The community <ArrowUpRight size={15} /></a>
          {authReady && session && (
            <div className="account-chip">
              <span className="account-avatar">{session.user.displayName.slice(0, 1).toUpperCase()}</span>
              <span className="account-name">{session.user.displayName}</span>
              <button className="logout-button" onClick={handleLogout} aria-label="Log out" title="Log out"><LogOut size={15} /></button>
            </div>
          )}
        </div>
      </header>

      <section className="hero">
        <div className="hero-copy">
          <p className="eyebrow"><span className="eyebrow-line" /> A LITTLE MORE ROOM AT THE TABLE</p>
          <h1>Good food<br />is better <em>shared.</em></h1>
          <p className="hero-intro">Making something lovely today? Let a neighbour have a plate. Share what’s cooking and how many servings you have to spare.</p>
          {session ? (
            <>
              <button className="primary-button" onClick={() => setIsFormOpen(true)}>
                <Plus size={17} /> Share what you’re making
              </button>
              <div className="hero-note"><span className="note-dot" /> Signed in as {session.user.displayName}</div>
            </>
          ) : (
            <div className="hero-note"><span className="note-dot" /> Sign in to share a dish with your neighbours.</div>
          )}
        </div>
        {!authReady || session ? (
          <div className="hero-art" aria-label="Illustration of a shared meal">
            <div className="art-sun" />
            <div className="art-plant plant-left"><i /><i /><i /><b /></div>
            <div className="art-plant plant-right"><i /><i /><i /><b /></div>
            <div className="table-shadow" />
            <div className="plate plate-back"><span /></div>
            <div className="plate plate-front">
              <span className="plate-rim"><span className="food food-one" /><span className="food food-two" /><span className="food food-three" /><span className="food food-four" /><span className="herb herb-one" /><span className="herb herb-two" /><span className="herb herb-three" /></span>
            </div>
            <div className="art-card"><span className="card-spark">✳</span><span>made with<br /><strong>a little extra love</strong></span></div>
            <div className="art-label"><span className="label-dot" /> MADE TO SHARE</div>
          </div>
        ) : (
          <section className="auth-panel" aria-labelledby="auth-title">
            <div className="auth-heading">
              <p className="eyebrow"><span className="eyebrow-line" /> YOUR PLACE AT THE TABLE</p>
              <h2 id="auth-title">{isRegistering ? 'Join the neighbourhood.' : 'Welcome back.'}</h2>
              <p>{isRegistering ? 'Create an account to share what you’re cooking.' : 'Sign in to share a meal with your neighbours.'}</p>
            </div>
            <form onSubmit={handleAuthentication}>
              {isRegistering && (
                <>
                  <label htmlFor="displayName">Your name</label>
                  <input id="displayName" name="displayName" maxLength="70" placeholder="e.g. Jamie" autoComplete="name" required />
                </>
              )}
              <label htmlFor="email">Email address</label>
              <input id="email" name="email" type="email" maxLength="254" placeholder="you@example.com" autoComplete="email" required />
              <label htmlFor="password">Password</label>
              <input id="password" name="password" type="password" minLength="8" maxLength="72" placeholder="At least 8 characters" autoComplete={isRegistering ? 'new-password' : 'current-password'} required />
              {authError && <p className="form-error" role="alert">{authError}</p>}
              <button className="primary-button auth-submit" type="submit" disabled={isAuthenticating}>
                {isAuthenticating ? 'One moment...' : isRegistering ? 'Create your account' : 'Sign in'}
              </button>
            </form>
            <p className="auth-switch">
              {isRegistering ? 'Already have an account?' : 'New to the neighbourhood?'}
              <button onClick={() => { setIsRegistering((current) => !current); setAuthError(''); }}>
                {isRegistering ? 'Sign in' : 'Create an account'}
              </button>
            </p>
            <p className="auth-footnote">Your details stay private. Only your name appears on food posts.</p>
          </section>
        )}
      </section>

      <section className="community" id="community">
        <div className="section-heading">
          <div>
            <p className="eyebrow"><span className="eyebrow-line" /> FROM YOUR NEIGHBOURS</p>
            <h2>On the table <span>today</span></h2>
          </div>
          <div className="section-aside"><span className="community-icon"><Users size={17} /></span><span>Good things are<br />happening nearby</span></div>
        </div>
        {error && <div className="notice" role="alert">{error} <button onClick={loadPosts}>Try again</button></div>}
        {isLoading ? (
          <div className="empty-state"><div className="empty-icon"><Soup size={23} /></div><p>Setting the table...</p></div>
        ) : posts.length === 0 ? (
          <div className="empty-state">
            <div className="empty-icon"><ChefHat size={23} /></div>
            <p>It’s a little quiet around the table.</p>
            <span>{session ? 'Be the first to share something delicious today.' : 'Sign in to be the first to share something delicious.'}</span>
            {session && <button className="text-button" onClick={() => setIsFormOpen(true)}>Share your first dish <ArrowDown size={15} /></button>}
          </div>
        ) : (
          <div className="post-grid">
            {posts.map((post, index) => (
              <article className={`post-card card-tone-${index % 3}`} key={post.id}>
                <div className="post-card-top"><span className="dish-icon"><Soup size={21} /></span><span className="today-tag"><span /> TODAY</span></div>
                <h3>{post.foodName}</h3>
                <p className="post-description">{post.description || 'Made with care and ready to be shared.'}</p>
                <div className="post-card-bottom">
                  <span className="servings"><Users size={15} /> {post.servings} {post.servings === 1 ? 'serving' : 'servings'} to share</span>
                  <span className="post-author">by {post.ownerName}</span>
                  <Heart size={17} className="heart-icon" />
                </div>
              </article>
            ))}
          </div>
        )}
        <div className="community-footer"><span>GOOD FOOD, GOOD PEOPLE</span><span className="footer-rule" /><span>PASS IT AROUND <Heart size={12} /></span></div>
      </section>

      <footer><a className="brand footer-brand" href="#"><span className="brand-mark"><Soup size={17} /></span><span>good neighbour<span className="brand-dot">.</span></span></a><span>Made for the moments we share.</span><span className="footer-phase">PHASE ONE · FOOD SHARING</span></footer>

      {isFormOpen && session && (
        <div className="modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) setIsFormOpen(false); }}>
          <section className="post-modal" role="dialog" aria-modal="true" aria-labelledby="modal-title">
            <div className="modal-heading"><div><p className="eyebrow"><span className="eyebrow-line" /> ADD A PLACE AT THE TABLE</p><h2 id="modal-title">What’s cooking?</h2></div><button className="close-button" aria-label="Close" onClick={() => setIsFormOpen(false)}><X size={20} /></button></div>
            <p className="modal-intro">Tell your neighbours what you’ve made and how many plates you can spare.</p>
            <form onSubmit={handleSubmit}>
              <label htmlFor="foodName">What are you making? <span>Required</span></label>
              <input id="foodName" name="foodName" maxLength="100" placeholder="e.g. Sunday tomato soup" required autoFocus />
              <label htmlFor="description">A little more about it <span>Optional</span></label>
              <textarea id="description" name="description" maxLength="500" placeholder="What’s in it? Any details to share?" rows="3" />
              <label htmlFor="servings">How many servings can you share? <span>Required</span></label>
              <div className="servings-input"><Users size={17} /><input id="servings" name="servings" type="number" min="1" max="1000" placeholder="4" required /><span>plates</span></div>
              {error && <p className="form-error" role="alert">{error}</p>}
              <button className="primary-button submit-button" type="submit" disabled={isSaving}>{isSaving ? 'Sharing your dish...' : <><Plus size={17} /> Share with neighbours</>}</button>
              <p className="privacy-note">Your name will appear on the post so neighbours know who made it.</p>
            </form>
          </section>
        </div>
      )}
    </main>
  );
}

export default App;
