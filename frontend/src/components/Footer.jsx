/**
 * Displays the footer
 */
export default function Footer({ itinerary}) {
    return (
    <footer className="app-footer">
      <div className="footer-content">
        <p>© 2026 DC Weekend Planner · Find your DMV vibe</p>
        <p className="github-link">
          Check out our project on 
          <a href="https://github.com/xuanbai01/dc-trip-planner/tree/main" target="_blank" rel="noopener noreferrer"> 👀GitHub</a>
        </p>
        <p className="photo-credit">
          Photo by <a href="https://unsplash.com/@andy_hehe?utm_source=unsplash&utm_medium=referral&utm_content=creditCopyText" target="_blank" rel="noopener noreferrer">Andy He</a> on <a href="https://unsplash.com/photos/body-of-water-near-trees-during-daytime-PuJc2Sodi94?utm_source=unsplash&utm_medium=referral&utm_content=creditCopyText" target="_blank" rel="noopener noreferrer">Unsplash</a>
        </p>
      </div>
    </footer>
  );
}