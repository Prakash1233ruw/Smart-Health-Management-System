# HTML reference UI

The React UI now follows the supplied `body-weather.html` and `SmartHealth – Login & Register.html` designs:

- Nunito typography, bundled locally so no Google Fonts request is needed.
- Indigo-to-peach login sky; blue doctor and purple admin variants, translucent 410px card, account tabs, role buttons, and password visibility control.
- Centered 400px × 780px phone workspace on desktop, full viewport on phones.
- Four background appearances, drifting clouds, breathing metric circles, translucent detail sheets, horizontal timeline stones, cream appointment tickets and bottom navigation.
- Reduced-motion preference disables cloud/orb animations. Forms and visit briefs scroll inside the phone layout.

The visual reference's mock data and fake authentication were not copied into production behavior. Login still uses the backend's username contract. Registration keeps the supported username/email, age or specialization, and password confirmation fields. There is no invented license-verification or doctor-approval workflow.

The old symptom analyzer remains retired. Its navigation position is now **Visit prep**, where patients can compose a reason and carry it into the real booking form. Nothing is saved until booking is confirmed. Doctors use it to open their appointment briefs. The other tabs are **Sky**, **River**, and **Tickets**. Existing pre-visit briefs, doctor approval, patient summaries, follow-up booking, measurements, unit correction and admin functions remain connected.

Sky appearance buttons change presentation only. Metric values/units/timestamps come from the backend; the UI does not apply the HTML's hard-coded diagnoses, normal ranges, health status claims or review badges.

The active browser suite includes `reference-ui.mjs` alongside booking and visit tests. It checks reference dimensions, side-by-side bottom tabs, actual API-backed metric selection, timeline selection, password visibility, visit-prep-to-booking, phone layout and reduced motion using intercepted fictional fixtures.
