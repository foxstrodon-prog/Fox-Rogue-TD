using System;
using System.IO;
using System.Diagnostics;
using System.Threading;
using System.Collections.Generic;
using System.Linq;

namespace RogueTDLauncher
{
    class Program
    {
        const string PackageName = "com.aistudio.roguetd.xtkp";
        const string MainActivityName = "com.example.MainActivity";
        const string ComponentName = PackageName + "/" + MainActivityName;

        static int Main(string[] args)
        {
            Console.Title = "Dual Front TD - ADB Companion Runner";
            Console.ForegroundColor = ConsoleColor.Cyan;
            Console.WriteLine("=================================================");
            Console.WriteLine("       DUAL FRONT TD - ADB COMPANION RUNNER      ");
            Console.WriteLine("=================================================");
            Console.ResetColor();

            try
            {
                string baseDir = AppDomain.CurrentDomain.BaseDirectory;

                // 1. Locate ADB
                Console.WriteLine("\n[1/5] Locating ADB (Android Debug Bridge)...");
                string adbPath = FindAdb(baseDir);
                if (string.IsNullOrEmpty(adbPath))
                {
                    ShowError("Could not find adb.exe. Please ensure Android SDK Platform Tools is installed or added to PATH.");
                    return 1;
                }
                Console.ForegroundColor = ConsoleColor.Green;
                Console.WriteLine("  -> Found ADB: " + adbPath);
                Console.ResetColor();

                // 2. Locate APK
                Console.WriteLine("\n[2/5] Locating Rogue TD APK...");
                string apkPath = FindApk(baseDir);
                if (string.IsNullOrEmpty(apkPath))
                {
                    ShowError("Could not find Rogue TD APK in 'apk/' or 'app/build/outputs/apk/debug/'.");
                    return 1;
                }
                Console.ForegroundColor = ConsoleColor.Green;
                Console.WriteLine("  -> Target APK: " + Path.GetFileName(apkPath));
                Console.ResetColor();

                // 3. Detect / Start Target Device or Emulator
                Console.WriteLine("\n[3/5] Checking connected devices / emulators...");
                string deviceSerial = GetOrCreateDevice(adbPath, baseDir);
                if (string.IsNullOrEmpty(deviceSerial))
                {
                    ShowError("No Android device or emulator could be found or started.");
                    return 1;
                }
                Console.ForegroundColor = ConsoleColor.Green;
                Console.WriteLine("  -> Connected to target device: " + deviceSerial);
                Console.ResetColor();

                // 4. Force Widescreen (Landscape) Orientation & Install APK
                Console.WriteLine("\n[4/5] Installing " + Path.GetFileName(apkPath) + "...");
                var installOutput = RunProcess(adbPath, string.Format("-s \"{0}\" install -r -d -g \"{1}\"", deviceSerial, apkPath), true);
                if (installOutput.Contains("INSTALL_FAILED_UPDATE_INCOMPATIBLE") ||
                    installOutput.Contains("signatures do not match") ||
                    installOutput.Contains("INSTALL_FAILED_CONFLICTING_PROVIDER") ||
                    installOutput.Contains("conflicts with an existing package"))
                {
                    Console.ForegroundColor = ConsoleColor.Yellow;
                    Console.WriteLine("  -> [Signature Conflict Detected] An existing installation with a conflicting certificate was found.");
                    Console.WriteLine("  -> Performing clean update (uninstalling mismatched build & installing new package)...");
                    Console.ResetColor();
                    RunProcess(adbPath, string.Format("-s \"{0}\" uninstall {1}", deviceSerial, PackageName), true);
                    RunProcess(adbPath, string.Format("-s \"{0}\" install -g \"{1}\"", deviceSerial, apkPath), true);
                }

                Console.WriteLine("  -> Configuring Widescreen Mode (Landscape)...");
                // Disable accelerometer auto-rotation and lock rotation to landscape (1 = 90 deg)
                RunProcess(adbPath, string.Format("-s \"{0}\" shell settings put system accelerometer_rotation 0", deviceSerial), false);
                RunProcess(adbPath, string.Format("-s \"{0}\" shell settings put system user_rotation 1", deviceSerial), false);

                // 5. Launch Game
                Console.WriteLine("\n[5/5] Launching Rogue TD in Widescreen Mode...");
                RunProcess(adbPath, string.Format("-s \"{0}\" shell am start -n {1}", deviceSerial, ComponentName), true);

                Console.ForegroundColor = ConsoleColor.Green;
                Console.WriteLine("\n=================================================");
                Console.WriteLine("  SUCCESS: Rogue TD is running in Widescreen!    ");
                Console.WriteLine("=================================================");
                Console.ResetColor();

                Thread.Sleep(2500);
                return 0;
            }
            catch (Exception ex)
            {
                ShowError("An unexpected error occurred: " + ex.Message);
                return 1;
            }
        }

        static string FindAdb(string baseDir)
        {
            string[] searchPaths = new string[]
            {
                Path.Combine(baseDir, "adb.exe"),
                Path.Combine(baseDir, "platform-tools", "adb.exe"),
                Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), @"Android\Sdk\platform-tools\adb.exe"),
                Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.UserProfile), @"AppData\Local\Android\Sdk\platform-tools\adb.exe"),
                @"C:\Android\sdk\platform-tools\adb.exe"
            };

            foreach (var p in searchPaths)
            {
                if (File.Exists(p)) return p;
            }

            var pathEnv = Environment.GetEnvironmentVariable("PATH") ?? "";
            foreach (var segment in pathEnv.Split(';'))
            {
                var trimmed = segment.Trim();
                if (!string.IsNullOrEmpty(trimmed))
                {
                    try
                    {
                        var test = Path.Combine(trimmed, "adb.exe");
                        if (File.Exists(test)) return test;
                    }
                    catch {}
                }
            }

            return null;
        }

        static string FindApk(string baseDir)
        {
            // If running directly inside the apk/ directory
            try
            {
                var localApks = Directory.GetFiles(baseDir, "*.apk")
                                         .OrderByDescending(f => File.GetLastWriteTime(f))
                                         .ToList();
                if (localApks.Count > 0) return localApks[0];
            }
            catch {}

            string apkDir = Path.Combine(baseDir, "apk");
            if (!Directory.Exists(apkDir))
            {
                try
                {
                    var parent = Directory.GetParent(baseDir);
                    if (parent != null)
                    {
                        string parentApk = Path.Combine(parent.FullName, "apk");
                        if (Directory.Exists(parentApk)) apkDir = parentApk;
                    }
                }
                catch {}
            }

            if (Directory.Exists(apkDir))
            {
                var apks = Directory.GetFiles(apkDir, "*.apk")
                                    .OrderByDescending(f => File.GetLastWriteTime(f))
                                    .ToList();
                if (apks.Count > 0) return apks[0];
            }

            string debugApk = Path.Combine(baseDir, @"app\build\outputs\apk\debug\app-debug.apk");
            if (File.Exists(debugApk)) return debugApk;

            return null;
        }

        static string GetOrCreateDevice(string adbPath, string baseDir)
        {
            var output = RunProcess(adbPath, "devices", false);
            var lines = output.Split(new[] { '\r', '\n' }, StringSplitOptions.RemoveEmptyEntries);
            var attached = new List<string>();

            for (int i = 1; i < lines.Length; i++)
            {
                var line = lines[i].Trim();
                if (string.IsNullOrEmpty(line)) continue;

                var tabIdx = line.LastIndexOf('\t');
                string serial = null;
                string status = null;

                if (tabIdx > 0)
                {
                    serial = line.Substring(0, tabIdx).Trim();
                    status = line.Substring(tabIdx + 1).Trim();
                }
                else
                {
                    var lastSpace = line.LastIndexOf(' ');
                    if (lastSpace > 0)
                    {
                        serial = line.Substring(0, lastSpace).Trim();
                        status = line.Substring(lastSpace + 1).Trim();
                    }
                }

                if (status == "device" && !string.IsNullOrEmpty(serial))
                {
                    attached.Add(serial);
                }
            }

            if (attached.Count > 0)
            {
                var emu = attached.FirstOrDefault(s => s.StartsWith("emulator-"));
                return emu ?? attached[0];
            }

            Console.WriteLine("  -> No active device found. Attempting to launch Android Emulator...");
            string emuPath = FindEmulator(baseDir);
            if (!string.IsNullOrEmpty(emuPath))
            {
                var avdListOut = RunProcess(emuPath, "-list-avds", false);
                var avds = avdListOut.Split(new[] { '\r', '\n' }, StringSplitOptions.RemoveEmptyEntries);
                if (avds.Length > 0)
                {
                    string targetAvd = avds[0].Trim();
                    Console.WriteLine("  -> Starting AVD: " + targetAvd + "...");
                    ProcessStartInfo psi = new ProcessStartInfo(emuPath, "-avd " + targetAvd)
                    {
                        UseShellExecute = false,
                        CreateNoWindow = false
                    };
                    Process.Start(psi);

                    Console.WriteLine("  -> Waiting for emulator to boot (this may take a few seconds)...");
                    RunProcess(adbPath, "wait-for-device", true);

                    for (int retry = 0; retry < 60; retry++)
                    {
                        var boot = RunProcess(adbPath, "shell getprop sys.boot_completed", false).Trim();
                        if (boot == "1")
                        {
                            Console.WriteLine("  -> Emulator booted successfully.");
                            Thread.Sleep(1000);
                            return GetOrCreateDevice(adbPath, baseDir);
                        }
                        Thread.Sleep(1000);
                    }
                }
            }

            return null;
        }

        static string FindEmulator(string baseDir)
        {
            string[] searchPaths = new string[]
            {
                Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), @"Android\Sdk\emulator\emulator.exe"),
                Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.UserProfile), @"AppData\Local\Android\Sdk\emulator\emulator.exe"),
                @"C:\Android\sdk\emulator\emulator.exe"
            };

            foreach (var p in searchPaths)
            {
                if (File.Exists(p)) return p;
            }

            return null;
        }

        static string RunProcess(string exe, string args, bool echo)
        {
            ProcessStartInfo psi = new ProcessStartInfo(exe, args)
            {
                RedirectStandardOutput = true,
                RedirectStandardError = true,
                UseShellExecute = false,
                CreateNoWindow = true
            };

            using (var proc = Process.Start(psi))
            {
                string output = proc.StandardOutput.ReadToEnd();
                string error = proc.StandardError.ReadToEnd();
                proc.WaitForExit();

                if (echo)
                {
                    if (!string.IsNullOrWhiteSpace(output))
                    {
                        foreach (var line in output.Split(new[] { '\r', '\n' }, StringSplitOptions.RemoveEmptyEntries))
                        {
                            Console.WriteLine("     " + line);
                        }
                    }
                    if (!string.IsNullOrWhiteSpace(error) && proc.ExitCode != 0)
                    {
                        Console.ForegroundColor = ConsoleColor.Yellow;
                        Console.WriteLine("     [Warning] " + error.Trim());
                        Console.ResetColor();
                    }
                }

                return output;
            }
        }

        static void ShowError(string message)
        {
            Console.ForegroundColor = ConsoleColor.Red;
            Console.WriteLine("\n[ERROR] " + message);
            Console.ResetColor();
            Console.WriteLine("\nPress any key to exit...");
            try
            {
                Console.ReadKey();
            }
            catch {}
        }
    }
}
