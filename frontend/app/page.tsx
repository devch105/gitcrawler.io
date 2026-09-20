import Image from "next/image";
import { ModeToggle } from '../components/ui/mode-toggle';

export default function Home() {
  return (
    <div className="flex flex-col flex-1 items-center justify-center bg-zinc-50 font-sans dark:bg-black">
      <ModeToggle/>
    </div>
  );
}
