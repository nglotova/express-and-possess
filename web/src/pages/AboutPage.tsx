import { Link } from "react-router";
import { useMe } from "../api/queries";

const USES = [
  {
    icon: "🎁",
    title: "Family and friends",
    text:
      "Everyone writes down what they would like for a birthday or the holidays. The others pick something to give, " +
      "and nobody ends up buying the same present twice.",
  },
  {
    icon: "💍",
    title: "Weddings and baby showers",
    text:
      "The couple lists the gifts they hope for and shares a link with the guests. Each guest reserves the gift they " +
      "will bring, and everyone sees what is still free.",
  },
  {
    icon: "💻",
    title: "At work",
    text:
      "Staff ask for a laptop, a monitor, a licence or access to a system. The right department takes the request, " +
      "marks it done when it is ready, and the person confirms it arrived.",
  },
  {
    icon: "✅",
    title: "Shared to-do lists",
    text:
      "Chores at home, tasks for volunteers, jobs before a move. People take on the tasks they can do, and the list " +
      "shows who is doing what.",
  },
];

const STEPS = [
  ["Create a group", "Invite people by email, or share a link anyone can join with."],
  ["Express a wish", "Describe it, add shop links and a picture, and say when you would like it by."],
  [
    "Someone takes care of it",
    "A member presses “I’ll take care of it”. Only one person can, so nothing is done twice. " +
      "They can stay incognito to keep a surprise.",
  ],
  ["Provided", "When it is bought, made or done, they mark it provided."],
  ["In possession", "You confirm “Received with thanks”, and the wish is fulfilled."],
];

/** What the app is for and how it works. Public, so visitors can read it before registering. */
export function AboutPage() {
  const me = useMe();
  return (
    <main className="page about">
      <p className="parent">
        {me.data ? <Link to="/">← My Groups</Link> : <Link to="/login">← Log in</Link>}
      </p>
      <h1 className="brand-title">Express &amp; Possess</h1>
      <p className="lead">
        A shared wish list for a group of people. Members say what they want, and the others help make it happen.
      </p>

      <section className="section">
        <h2>What it can be used for</h2>
        <div className="uses">
          {USES.map((u) => (
            <div key={u.title} className="use">
              <span className="use-icon" aria-hidden="true">
                {u.icon}
              </span>
              <h3>{u.title}</h3>
              <p>{u.text}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="section">
        <h2>How it works</h2>
        <ol className="steps">
          {STEPS.map(([title, text]) => (
            <li key={title}>
              <strong>{title}.</strong> {text}
            </li>
          ))}
        </ol>
        <p className="muted">
          The group hears about every new wish, and you hear about each step of yours, in the app and by email. Each
          wish has its own comments for questions such as size or colour. Groups are private: only their members see
          the wishes.
        </p>
      </section>

      {!me.data && (
        <div className="button-row section">
          <Link to="/register" className="button primary">
            Create an account
          </Link>
          <Link to="/login" className="button">
            Log in
          </Link>
        </div>
      )}
    </main>
  );
}
