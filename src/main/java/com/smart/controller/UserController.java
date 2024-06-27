package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private ContactRepository contactRepository;
	
	//adding common data
	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {
		String userName = principal.getName();
		System.out.println("USERNAME"+userName);
		
		User user = this.userRepository.getUserByUserName(userName);
		System.out.println("USER "+ user);
		
		model.addAttribute("user",user);
	}
	
	//home  dashbord
	@GetMapping("/index")
	public String dashboard(Model model, Principal principal){
		model.addAttribute("title", "User Dashboard");
		return "normal/user_dashboard";
	}
	
	//add contact handler
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model) {
		model.addAttribute("title", "Add Contact");
		model.addAttribute("contact", new Contact());
		return "normal/add_contact_form";
		
	}
	
	//process add contact form
	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute Contact contact, @RequestParam("image1") MultipartFile file, Principal principal, HttpSession session) {
		
		try {
			String name = principal.getName();
			User user = this.userRepository.getUserByUserName(name);
			
			if(file.isEmpty()) {
				System.out.println("file not given");
				contact.setImage("contact.png");
			}else {
				contact.setImage(file.getOriginalFilename());
				
				File file2 = new ClassPathResource("/static/image").getFile();
				Path path = Paths.get(file2.getAbsolutePath()+File.separator+file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				System.out.println("image uploaded");
			}
			
			contact.setUser(user);
			
			user.getContacts().add(contact);
			this.userRepository.save(user);
			
			System.out.println("DATA"+contact);
			System.out.println("added to data base.");
			
			session.setAttribute("message", new Message("Your contact is added!! and more..", "success"));
			
		} catch (Exception e) {
			System.out.println("ERROR"+e.getMessage());
			e.printStackTrace();
			session.setAttribute("message", new Message("Something want Wrong!! Try again..", "danger"));
			
		}

		return "normal/add_contact_form";
	}
	
	//view handler
	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page, Model model, Principal principal) {
		model.addAttribute("title", "Show User Contacts");
		String name = principal.getName();
		User user = this.userRepository.getUserByUserName(name);
		
		Pageable pageable = PageRequest.of(page, 5);
		Page<Contact> contacts = this.contactRepository.findContactsByUser(user.getId(), pageable);
		model.addAttribute("contacts", contacts);
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", contacts.getTotalPages());
		
		return "normal/show_contacts";
	}
	
	//detail view handler
	@GetMapping("/{cId}/contact")
	public String showContactDetail(@PathVariable("cId") Integer cId, Model model,Principal principal) {
		System.out.println("cid "+cId);
		Optional<Contact> contactOp = this.contactRepository.findById(cId);
		Contact contact = contactOp.get();
		
		String name = principal.getName();
		User user = this.userRepository.getUserByUserName(name);
		
		if (user.getId()==contact.getUser().getId()) {
			model.addAttribute("title", contact.getName());
			model.addAttribute("contact", contact);
		}
		
		return "normal/contact_detail";
	}
	
	//delete contact handler
	@GetMapping("/delete/{cId}")
	public String  deleteContact(@PathVariable("cId") Integer cId, Principal principal, HttpSession session) {
		
		Contact contact = this.contactRepository.findById(cId).get();
		User user = this.userRepository.getUserByUserName( principal.getName());
		
		user.getContacts().remove(contact);
		this.userRepository.save(user);
		
		session.setAttribute("message", new Message("contact deleted successfully", "success"));
		
		return "redirect:/user/show-contacts/0";
	}
	
	//update contact handler
	@PostMapping("/update-contact/{cId}")
	public String updateForm(@PathVariable("cId") Integer cId, Model model) {
		model.addAttribute("title", "Update Contact");
		Contact contact = this.contactRepository.findById(cId).get();
		model.addAttribute("contact", contact);
		return "normal/update_form";
	}
	
	
	//update contact handler
	@PostMapping("/process-update")
	public String updateHandler(@ModelAttribute Contact contact, @RequestParam("image1") MultipartFile file, Principal principal, Model model, HttpSession session) {
		try {
			Contact oldcontact = this.contactRepository.findById(contact.getcId()).get();
			if(!file.isEmpty()) {
				
				File filedelete = new ClassPathResource("/static/image").getFile();
				File file1= new File(filedelete,oldcontact.getImage());
				file1.delete();
				
				File file2 = new ClassPathResource("/static/image").getFile();
				Path path = Paths.get(file2.getAbsolutePath()+File.separator+file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				contact.setImage(file.getOriginalFilename());
			} else {
				contact.setImage(oldcontact.getImage());
			}
			User user= this.userRepository.getUserByUserName(principal.getName());
			contact.setUser(user);
			this.contactRepository.save(contact);
			session.setAttribute("message", new Message("Your contact successfully updated...", "success"));
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			session.setAttribute("message", new Message("something want wrong!!your contact not update...", "danger"));
		}
		System.out.println("CONTACT NAME "+contact.getName());
		return "redirect:/user/"+contact.getcId()+"/contact";
	}
	
	//your profile handler
	@GetMapping("/profile")
	public String yourProfile(Model model) {
		model.addAttribute("title", "Profile Page");
		return "normal/profile";
	}
}