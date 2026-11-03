import customtkinter as ctk
import tkinter as tk
from tkinter import messagebox
from PIL import Image, ImageTk
import os
import shutil
import re

class CropWindow(ctk.CTkToplevel):
    def __init__(self, folder_path, webinar_name, mode, parent):
        super().__init__(parent)
        
        self.folder_path = folder_path
        self.webinar_name = webinar_name
        self.mode = mode
        self.parent = parent
        
        self.title(f"Обрезка: {webinar_name}")
        
        screen_width = self.winfo_screenwidth()
        screen_height = self.winfo_screenheight()
        self.geometry(f"{screen_width}x{screen_height - 80}+0+0")
        self.minsize(600, 400)
        self.transient(parent)
        self.lift()
        self.focus_force()
        
        self.image_files = []
        self.current_index = 0
        self.current_image = None
        self.image_x = 0
        self.image_y = 0
        self.image_w = 0
        self.image_h = 0
        
        self.crop_box = None 
        self.drag_mode = None
        self.start_x = 0
        self.start_y = 0
        self.start_box = None
        self.min_size = 50
        self.handle_size = 8
        self.is_processing = False
        
        # Статистика
        self.total_orig_size = 0
        self.total_crop_size = 0
        self.total_processed = 0
        # total_skipped больше не нужен, будем вычислять на лету
        
        self.protocol("WM_DELETE_WINDOW", self.show_report_and_close)
        
        try:
            valid_exts = ('.jpg', '.jpeg', '.png')
            self.image_files = sorted([
                f for f in os.listdir(self.folder_path) 
                if f.lower().endswith(valid_exts)
            ])
        except Exception as e:
            print(f"Ошибка чтения папки: {e}")
            
        if not self.image_files:
            ctk.CTkLabel(self, text="В папке нет изображений", font=ctk.CTkFont(size=20)).pack(pady=50)
            return

        self.canvas = tk.Canvas(self, bg="black", highlightthickness=0)
        self.canvas.pack(fill="both", expand=True, padx=10, pady=10)
        
        self.bottom_frame = ctk.CTkFrame(self)
        self.bottom_frame.pack(fill="x", padx=10, pady=(0, 10))
        
        self.btn_first = ctk.CTkButton(self.bottom_frame, text="<<", width=50, font=ctk.CTkFont(weight="bold"), text_color="white", command=self.go_first)
        self.btn_first.pack(side="left", padx=5)
        
        self.btn_prev = ctk.CTkButton(self.bottom_frame, text="<", width=50, font=ctk.CTkFont(weight="bold"), text_color="white", command=self.go_prev)
        self.btn_prev.pack(side="left", padx=5)
        
        self.btn_next = ctk.CTkButton(self.bottom_frame, text=">", width=50, font=ctk.CTkFont(weight="bold"), text_color="white", command=self.go_next)
        self.btn_next.pack(side="left", padx=5)
        
        self.btn_last = ctk.CTkButton(self.bottom_frame, text=">>", width=50, font=ctk.CTkFont(weight="bold"), text_color="white", command=self.go_last)
        self.btn_last.pack(side="left", padx=5)
        
        self.btn_done = ctk.CTkButton(self.bottom_frame, text="ГОТОВО", fg_color="#FF9F43", font=ctk.CTkFont(weight="bold"), text_color="white", command=self.show_report_and_close)
        self.btn_done.pack(side="right", padx=5)
        
        self.btn_crop_all = ctk.CTkButton(self.bottom_frame, text="Crop it ALL", fg_color="#FF9F43", font=ctk.CTkFont(weight="bold"), text_color="white", command=self.crop_all_images)
        self.btn_crop_all.pack(side="right", padx=5)

        self.btn_crop = ctk.CTkButton(self.bottom_frame, text="Crop it", fg_color="#0F9D8E", font=ctk.CTkFont(weight="bold"), text_color="white", command=self.crop_current_image)
        self.btn_crop.pack(side="right", padx=5)
        
        self.canvas.bind("<Configure>", self.on_resize)
        self.canvas.bind("<ButtonPress-1>", self.on_button_press)
        self.canvas.bind("<B1-Motion>", self.on_mouse_drag)
        self.canvas.bind("<ButtonRelease-1>", self.on_button_release)
        
        self.after(100, self.load_image)

    def show_report_and_close(self):
        """Показывает итоговый отчет и закрывает окно"""
        if self.is_processing:
            self.destroy()
            return
            
        # Пропущенные — это все файлы, которые остались в списке (не были обрезаны)
        skipped_count = len(self.image_files)
        
        if self.total_processed > 0 or skipped_count > 0:
            saved_bytes = self.total_orig_size - self.total_crop_size
            if saved_bytes > 1024 * 1024:
                saved_str = f"{saved_bytes / (1024 * 1024):.1f} МБ"
            else:
                saved_str = f"{saved_bytes / 1024:.1f} КБ"
                
            orig_str = f"{self.total_orig_size / (1024 * 1024):.1f} МБ"
            crop_str = f"{self.total_crop_size / (1024 * 1024):.1f} МБ"

            report = (f"Обрезано файлов: {self.total_processed}\n"
                      f"Пропущено: {skipped_count}\n\n"
                      f"Исходники занимали: {orig_str}\n"
                      f"Обрезанные занимают: {crop_str}\n\n")
                      
            if self.mode == "delete":
                report += f"Свободная память увеличена на: {saved_str}"
            else:
                report += f"Будет освобождено после очистки Корзины: {saved_str}"
                
            messagebox.showinfo("Обрезка завершена", report)
        
        self.destroy()

    def on_resize(self, event):
        if self.current_image and not self.is_processing:
            self.display_image()

    def load_image(self):
        if not self.image_files:
            return
            
        filename = self.image_files[self.current_index]
        filepath = os.path.join(self.folder_path, filename)
        
        try:
            img = Image.open(filepath)
            self.current_image = img
            self.display_image()
            self.update_nav_buttons()
        except Exception as e:
            print(f"Ошибка загрузки изображения: {e}")

    def display_image(self):
        canvas_width = self.canvas.winfo_width()
        canvas_height = self.canvas.winfo_height()
        
        if canvas_width > 1 and canvas_height > 1:
            img_ratio = self.current_image.width / self.current_image.height
            canvas_ratio = canvas_width / canvas_height
            
            if canvas_ratio > img_ratio:
                self.image_h = canvas_height
                self.image_w = int(self.image_h * img_ratio)
            else:
                self.image_w = canvas_width
                self.image_h = int(self.image_w / img_ratio)
                
            resized_img = self.current_image.resize((self.image_w, self.image_h), Image.Resampling.LANCZOS)
            self.tk_image = ImageTk.PhotoImage(resized_img)
            
            self.canvas.delete("all")
            self.image_x = (canvas_width - self.image_w) // 2
            self.image_y = (canvas_height - self.image_h) // 2
            self.canvas.create_image(self.image_x, self.image_y, anchor="nw", image=self.tk_image)
            
            self.title(f"[{self.current_index + 1}/{len(self.image_files)}] - {self.webinar_name}")
            
            if self.crop_box is None:
                inset_x = int(self.image_w * 0.1)
                inset_y = int(self.image_h * 0.1)
                self.crop_box = (
                    self.image_x + inset_x, 
                    self.image_y + inset_y, 
                    self.image_x + self.image_w - inset_x, 
                    self.image_y + self.image_h - inset_y
                )
            
            self.redraw_crop_ui()

    def redraw_crop_ui(self):
        self.canvas.delete("crop_ui")
        if not self.crop_box: return
            
        x1, y1, x2, y2 = self.crop_box
        canvas_w = self.canvas.winfo_width()
        canvas_h = self.canvas.winfo_height()
        
        self.canvas.create_rectangle(0, 0, canvas_w, y1, fill="black", stipple="gray50", outline="", tags="crop_ui")
        self.canvas.create_rectangle(0, y2, canvas_w, canvas_h, fill="black", stipple="gray50", outline="", tags="crop_ui")
        self.canvas.create_rectangle(0, y1, x1, y2, fill="black", stipple="gray50", outline="", tags="crop_ui")
        self.canvas.create_rectangle(x2, y1, canvas_w, y2, fill="black", stipple="gray50", outline="", tags="crop_ui")
        
        self.canvas.create_rectangle(x1, y1, x2, y2, outline="white", width=2, tags="crop_ui")
        
        mid_x = (x1 + x2) / 2
        mid_y = (y1 + y2) / 2
        handles = [(x1, y1), (mid_x, y1), (x2, y1), (x1, mid_y), (x2, mid_y), (x1, y2), (mid_x, y2), (x2, y2)]
        for hx, hy in handles:
            self.canvas.create_oval(hx - self.handle_size, hy - self.handle_size, 
                                    hx + self.handle_size, hy + self.handle_size, 
                                    fill="white", outline="black", width=1, tags="crop_ui")

    def get_handle_at(self, x, y):
        if not self.crop_box: return "outside"
        x1, y1, x2, y2 = self.crop_box
        mid_x = (x1 + x2) / 2
        mid_y = (y1 + y2) / 2
        hs = self.handle_size + 5
        
        if abs(x - x1) < hs and abs(y - y1) < hs: return "top_left"
        if abs(x - x2) < hs and abs(y - y1) < hs: return "top_right"
        if abs(x - x1) < hs and abs(y - y2) < hs: return "bottom_left"
        if abs(x - x2) < hs and abs(y - y2) < hs: return "bottom_right"
        if abs(x - mid_x) < hs and abs(y - y1) < hs: return "top"
        if abs(x - mid_x) < hs and abs(y - y2) < hs: return "bottom"
        if abs(x - x1) < hs and abs(y - mid_y) < hs: return "left"
        if abs(x - x2) < hs and abs(y - mid_y) < hs: return "right"
        if x1 < x < x2 and y1 < y < y2: return "center"
        return "outside"

    def on_button_press(self, event):
        if self.is_processing: return
        self.drag_mode = self.get_handle_at(event.x, event.y)
        self.start_x = event.x
        self.start_y = event.y
        if self.drag_mode == "outside":
            self.crop_box = (event.x, event.y, event.x, event.y)
            self.redraw_crop_ui()
        else:
            self.start_box = self.crop_box

    def on_mouse_drag(self, event):
        if self.is_processing or not self.crop_box: return
        dx = event.x - self.start_x
        dy = event.y - self.start_y
        x1, y1, x2, y2 = self.start_box if self.drag_mode != "outside" else self.crop_box
        
        if self.drag_mode == "outside":
            self.crop_box = (self.start_x, self.start_y, event.x, event.y)
        elif self.drag_mode == "center":
            w = x2 - x1
            h = y2 - y1
            new_x1 = max(0, min(self.start_box[0] + dx, self.canvas.winfo_width() - w))
            new_y1 = max(0, min(self.start_box[1] + dy, self.canvas.winfo_height() - h))
            self.crop_box = (new_x1, new_y1, new_x1 + w, new_y1 + h)
        else:
            new_x1, new_y1, new_x2, new_y2 = self.start_box
            if "left" in self.drag_mode: new_x1 += dx
            if "right" in self.drag_mode: new_x2 += dx
            if "top" in self.drag_mode: new_y1 += dy
            if "bottom" in self.drag_mode: new_y2 += dy
            
            if new_x2 - new_x1 < self.min_size:
                if "left" in self.drag_mode: new_x1 = new_x2 - self.min_size
                else: new_x2 = new_x1 + self.min_size
            if new_y2 - new_y1 < self.min_size:
                if "top" in self.drag_mode: new_y1 = new_y2 - self.min_size
                else: new_y2 = new_y1 + self.min_size
                
            new_x1 = max(0, new_x1)
            new_y1 = max(0, new_y1)
            new_x2 = min(self.canvas.winfo_width(), new_x2)
            new_y2 = min(self.canvas.winfo_height(), new_y2)
            self.crop_box = (new_x1, new_y1, new_x2, new_y2)
            
        self.redraw_crop_ui()

    def on_button_release(self, event):
        if self.is_processing: return
        if self.crop_box:
            x1, y1, x2, y2 = self.crop_box
            self.crop_box = (min(x1, x2), min(y1, y2), max(x1, x2), max(y1, y2))
            self.redraw_crop_ui()
        self.drag_mode = None
        self.start_box = None

    def get_next_filename(self, target_folder):
        max_num = 0
        for f in os.listdir(target_folder):
            match = re.match(r'^(\d{3})\.jpg$', f)
            if match:
                num = int(match.group(1))
                if num > max_num: max_num = num
        return f"{max_num + 1:03d}.jpg"

    def get_original_crop_box(self):
        if not self.crop_box or not self.current_image: return None
        x1, y1, x2, y2 = self.crop_box
        scale = self.image_w / self.current_image.width 
        
        orig_x1 = int((x1 - self.image_x) / scale)
        orig_y1 = int((y1 - self.image_y) / scale)
        orig_x2 = int((x2 - self.image_x) / scale)
        orig_y2 = int((y2 - self.image_y) / scale)
        
        orig_x1 = max(0, orig_x1)
        orig_y1 = max(0, orig_y1)
        orig_x2 = min(self.current_image.width, orig_x2)
        orig_y2 = min(self.current_image.height, orig_y2)
        
        return (orig_x1, orig_y1, orig_x2, orig_y2)

    def crop_current_image(self):
        if self.is_processing: return
        crop_coords = self.get_original_crop_box()
        if not crop_coords:
            print("Сначала нарисуйте рамку")
            return

        filename = self.image_files[self.current_index]
        filepath = os.path.join(self.folder_path, filename)
        
        try:
            target_dir = os.path.join(self.folder_path, self.webinar_name)
            os.makedirs(target_dir, exist_ok=True)
            new_filename = self.get_next_filename(target_dir)
            save_path = os.path.join(target_dir, new_filename)
            
            orig_size = os.path.getsize(filepath)
            cropped_img = self.current_image.crop(crop_coords)
            cropped_img.save(save_path, "JPEG", quality=90)
            crop_size = os.path.getsize(save_path)
            
            self.total_orig_size += orig_size
            self.total_crop_size += crop_size
            self.total_processed += 1
            
            print(f"Сохранено: {new_filename}")
            
            self.process_source_file(filepath, filename)
                
            self.image_files.pop(self.current_index)
            
            if not self.image_files:
                messagebox.showinfo("Готово", "Все файлы обработаны!")
                self.show_report_and_close()
                return
                
            if self.current_index >= len(self.image_files):
                self.current_index = len(self.image_files) - 1
                
            self.load_image()
            
        except Exception as e:
            print(f"Ошибка при обрезке: {e}")

    def crop_all_images(self):
        if self.is_processing: return
        crop_coords = self.get_original_crop_box()
        if not crop_coords:
            messagebox.showwarning("Внимание", "Сначала нарисуйте рамку!")
            return

        if not messagebox.askyesno("Подтверждение", f"Обрезать все оставшиеся файлы ({len(self.image_files) - self.current_index} шт.)?"):
            return

        self.is_processing = True
        self.btn_crop.configure(state="disabled")
        self.btn_crop_all.configure(state="disabled")

        target_dir = os.path.join(self.folder_path, self.webinar_name)
        os.makedirs(target_dir, exist_ok=True)
        
        files_to_process = self.image_files[self.current_index:]
        total = len(files_to_process)
        processed = 0

        for filename in files_to_process:
            filepath = os.path.join(self.folder_path, filename)
            try:
                orig_size = os.path.getsize(filepath)
                img = Image.open(filepath)
                cropped_img = img.crop(crop_coords)
                
                new_filename = self.get_next_filename(target_dir)
                save_path = os.path.join(target_dir, new_filename)
                cropped_img.save(save_path, "JPEG", quality=90)
                
                crop_size = os.path.getsize(save_path)
                
                self.total_orig_size += orig_size
                self.total_crop_size += crop_size
                self.total_processed += 1
                
                self.process_source_file(filepath, filename)
                
                processed += 1
                self.title(f"Обработка... {processed} из {total}")
                self.update_idletasks()
                
            except Exception as e:
                print(f"Ошибка с файлом {filename}: {e}")

        self.is_processing = False
        self.image_files.clear()
        self.show_report_and_close()

    def process_source_file(self, filepath, filename):
        if self.mode == "delete":
            os.remove(filepath)
        else:
            bin_dir = os.path.join(self.folder_path, "_Корзина")
            os.makedirs(bin_dir, exist_ok=True)
            base, ext = os.path.splitext(filename)
            counter = 1
            dest_path = os.path.join(bin_dir, filename)
            while os.path.exists(dest_path):
                dest_path = os.path.join(bin_dir, f"{base}_{counter}{ext}")
                counter += 1
            shutil.move(filepath, dest_path)

    def go_first(self):
        if self.is_processing: return
        self.current_index = 0
        self.load_image()

    def go_prev(self):
        if self.is_processing: return
        if self.current_index > 0:
            self.current_index -= 1
            self.load_image()

    def go_next(self):
        if self.is_processing: return
        if self.current_index < len(self.image_files) - 1:
            self.current_index += 1
            self.load_image()

    def go_last(self):
        if self.is_processing: return
        self.current_index = len(self.image_files) - 1
        self.load_image()
        
    def update_nav_buttons(self):
        if self.is_processing: return
        if self.current_index == 0:
            self.btn_first.configure(state="disabled")
            self.btn_prev.configure(state="disabled")
            self.btn_next.configure(state="normal")
            self.btn_last.configure(state="normal")
        elif self.current_index == len(self.image_files) - 1:
            self.btn_first.configure(state="normal")
            self.btn_prev.configure(state="normal")
            self.btn_next.configure(state="disabled")
            self.btn_last.configure(state="disabled")
        else:
            self.btn_first.configure(state="normal")
            self.btn_prev.configure(state="normal")
            self.btn_next.configure(state="normal")
            self.btn_last.configure(state="normal")