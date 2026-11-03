import customtkinter as ctk
import tkinter.filedialog
import tkinter as tk
from tkinter import messagebox
import os
import shutil
import re
import json

class CropItAllApp(ctk.CTk):
    def __init__(self):
        super().__init__()

        self.title("CropItAll")
        self.geometry("500x650")
        ctk.set_appearance_mode("system")
        
        self.folder_path = ""
        self.webinar_name = tk.StringVar()
        self.mode_var = tk.StringVar(value="bin") 

        self.primary_color = "#0F9D8E"
        
        # Файл настроек для запоминания папки
        self.settings_file = "settings.json"
        
        self.setup_ui()
        
        # Загружаем последнюю папку при запуске
        self.load_settings()

    def setup_ui(self):
        self.title_label = ctk.CTkLabel(
            self, text="CropItAll", 
            font=ctk.CTkFont(size=32, weight="bold"),
            text_color=self.primary_color
        )
        self.title_label.pack(pady=(30, 20))

        self.btn_select_folder = ctk.CTkButton(
            self, text="  ВЫБРАТЬ  ПАПКУ  ", command=self.select_folder,
            fg_color=self.primary_color, hover_color="#0A7A6E",
            font=ctk.CTkFont(weight="bold"), text_color="white"
        )
        self.btn_select_folder.pack(pady=10)

        self.lbl_folder_path = ctk.CTkLabel(
            self, text="Папка не выбрана", 
            text_color=("gray20", "gray80"),
            wraplength=400
        )
        self.lbl_folder_path.pack(pady=(0, 20))

        self.entry_webinar = ctk.CTkEntry(
            self, textvariable=self.webinar_name, 
            placeholder_text="Название вебинара", width=300
        )
        self.entry_webinar.pack(pady=10)
        
        self.webinar_name.trace_add("write", lambda *args: self.update_frame_button_state())

        self.lbl_mode = ctk.CTkLabel(self, text="Режим:", font=ctk.CTkFont(weight="bold"))
        self.lbl_mode.pack(pady=(20, 5))

        self.radio_bin = ctk.CTkRadioButton(
            self, text="Переместить в Корзину", variable=self.mode_var, value="bin",
            fg_color=self.primary_color
        )
        self.radio_bin.pack(pady=5)

        self.radio_delete = ctk.CTkRadioButton(
            self, text="Удалить исходники", variable=self.mode_var, value="delete",
            fg_color=self.primary_color
        )
        self.radio_delete.pack(pady=5)

        self.btn_frame = ctk.CTkButton(
            self, text="FRAME", font=ctk.CTkFont(size=18, weight="bold"),
            height=50, state="disabled", command=self.check_folder_and_start,
            fg_color=self.primary_color, hover_color="#0A7A6E",
            text_color="white", width=300
        )
        self.btn_frame.pack(pady=(40, 20))

    def load_settings(self):
        """Загружает последнюю папку при запуске"""
        if os.path.exists(self.settings_file):
            try:
                with open(self.settings_file, "r", encoding="utf-8") as f:
                    data = json.load(f)
                    folder = data.get("last_folder", "")
                    if folder and os.path.isdir(folder):
                        self.folder_path = folder
                        display_path = folder if len(folder) < 50 else "..." + folder[-47:]
                        self.lbl_folder_path.configure(text=display_path, text_color=("black", "white"))
                        self.update_frame_button_state()
            except Exception as e:
                print(f"Ошибка загрузки настроек: {e}")

    def save_settings(self):
        """Сохраняет путь к папке"""
        try:
            with open(self.settings_file, "w", encoding="utf-8") as f:
                json.dump({"last_folder": self.folder_path}, f, ensure_ascii=False, indent=4)
        except Exception as e:
            print(f"Ошибка сохранения настроек: {e}")

    def select_folder(self):
        path = tkinter.filedialog.askdirectory()
        if path:
            self.folder_path = path
            display_path = path if len(path) < 50 else "..." + path[-47:]
            self.lbl_folder_path.configure(text=display_path, text_color=("black", "white"))
            self.update_frame_button_state()
            self.save_settings() # Сохраняем выбранный путь
            self.check_trash_bin()

    def check_trash_bin(self):
        bin_path = os.path.join(self.folder_path, "_Корзина")
        if os.path.exists(bin_path) and os.path.isdir(bin_path):
            files = os.listdir(bin_path)
            if files:
                answer = messagebox.askyesno(
                    "Очистка Корзины", 
                    f"В Корзине найдено {len(files)} файлов.\nУдалить их, чтобы освободить место?"
                )
                if answer:
                    try:
                        shutil.rmtree(bin_path)
                        messagebox.showinfo("Готово", "Корзина очищена!")
                    except Exception as e:
                        messagebox.showerror("Ошибка", f"Не удалось очистить Корзину: {e}")

    def update_frame_button_state(self):
        has_folder = bool(self.folder_path)
        has_name = bool(self.webinar_name.get().strip())

        if has_folder and has_name:
            self.btn_frame.configure(state="normal")
        else:
            self.btn_frame.configure(state="disabled")

    def check_folder_and_start(self):
        webinar = self.webinar_name.get().strip()
        target_dir = os.path.join(self.folder_path, webinar)
        
        if os.path.exists(target_dir) and os.path.isdir(target_dir):
            files = [f for f in os.listdir(target_dir) if re.match(r'^\d{3}\.jpg$', f)]
            if files:
                max_num = max([int(f[:3]) for f in files]) if files else 0
                next_num = max_num + 1
                count = len(os.listdir(target_dir))
                
                dialog = ctk.CTkToplevel(self)
                dialog.title("Папка существует")
                dialog.geometry("450x220")
                dialog.transient(self)
                dialog.grab_set()
                
                self.update_idletasks()
                x = self.winfo_x() + (self.winfo_width() // 2) - 225
                y = self.winfo_y() + (self.winfo_height() // 2) - 110
                dialog.geometry(f"+{x}+{y}")

                lbl = ctk.CTkLabel(
                    dialog, 
                    text=f"Папка «{webinar}» уже существует.\nВ ней находится {count} файлов.\n\nВы можете создать новую папку, или записывать обрезанные слайды в папку «{webinar}», продолжив нумерацию файлов с {next_num:03d}.", 
                    wraplength=400,
                    justify="left"
                )
                lbl.pack(pady=(20, 10), padx=20)

                btn_frame = ctk.CTkFrame(dialog)
                btn_frame.pack(pady=10)

                def continue_crop():
                    dialog.destroy()
                    self.start_cropping()

                def change_name():
                    dialog.destroy()
                    self.entry_webinar.focus_set()
                    self.entry_webinar.select_range(0, 'end')

                ctk.CTkButton(btn_frame, text="ПРОДОЛЖИТЬ", command=continue_crop, fg_color="#0F9D8E", text_color="white", font=ctk.CTkFont(weight="bold")).pack(side="left", padx=10)
                ctk.CTkButton(btn_frame, text="ИЗМЕНИТЬ НАЗВАНИЕ", command=change_name, fg_color="#FF9F43", text_color="white", font=ctk.CTkFont(weight="bold")).pack(side="left", padx=10)
                
                return
                
        self.start_cropping()

    def start_cropping(self):
        from crop_window import CropWindow
        self.crop_window = CropWindow(
            self.folder_path,
            self.webinar_name.get().strip(),
            self.mode_var.get(),
            self
        )

if __name__ == "__main__":
    app = CropItAllApp()
    app.mainloop()