export default function App() {
  return (
    <div className="relative h-screen w-screen overflow-hidden bg-black flex flex-col text-white justify-between">
      <video
        autoPlay
        loop
        muted
        playsInline
        className="absolute inset-0 w-full h-full object-contain z-0 opacity-80"
      >
        <source
          src="https://d8j0ntlcm91z4.cloudfront.net/user_38xzZboKViGWJOttwIXH07lWA1P/hf_20260324_151826_c7218672-6e92-402c-9e45-f1e0f454bdc4.mp4"
          type="video/mp4"
        />
      </video>

      <nav className="relative z-10 w-full flex-shrink-0">
        <div className="flex flex-row items-center justify-between px-8 py-6 max-w-7xl mx-auto">
          <span style={{ fontFamily: "'Instrument Serif', serif" }} className="text-5xl tracking-tight text-white">
            Vidyā<sup className="text-base">®</sup>
          </span>
          <div className="flex gap-6 items-center">
            <a href="/login" className="text-lg text-white/80 hover:text-white transition-colors">Login</a>
            <a href="/registration" className="liquid-glass rounded-full px-8 py-3 text-lg text-white hover:scale-[1.03] transition-transform shadow-[0_0_15px_rgba(255,255,255,0.1)] border border-white/20">
              Sign Up
            </a>
          </div>
        </div>
      </nav>

      <main className="relative z-10 flex-1 flex flex-col items-center justify-center text-center px-6">
      </main>

      <footer className="relative z-10 w-full flex-shrink-0 pb-10">
        <div className="mx-auto flex max-w-7xl flex-wrap items-center justify-center gap-x-10 gap-y-5 px-6">
          <a href="/about" className="liquid-glass rounded-full border border-white/20 px-8 py-3 text-lg text-white/85 transition-transform transition-colors hover:scale-[1.03] hover:text-white shadow-[0_0_15px_rgba(255,255,255,0.1)]">
            About
          </a>
          <a href="/contact" className="liquid-glass rounded-full border border-white/20 px-8 py-3 text-lg text-white/85 transition-transform transition-colors hover:scale-[1.03] hover:text-white shadow-[0_0_15px_rgba(255,255,255,0.1)]">
            Contact
          </a>
          <a href="/courses" className="liquid-glass rounded-full border border-white/20 px-8 py-3 text-lg text-white/85 transition-transform transition-colors hover:scale-[1.03] hover:text-white shadow-[0_0_15px_rgba(255,255,255,0.1)]">
            Courses
          </a>
          <a href="/course/navigator" className="liquid-glass rounded-full border border-white/20 px-8 py-3 text-lg text-white/85 transition-transform transition-colors hover:scale-[1.03] hover:text-white shadow-[0_0_15px_rgba(255,255,255,0.1)]">
            Course Navigator
          </a>
        </div>
      </footer>
    </div>
  );
}
