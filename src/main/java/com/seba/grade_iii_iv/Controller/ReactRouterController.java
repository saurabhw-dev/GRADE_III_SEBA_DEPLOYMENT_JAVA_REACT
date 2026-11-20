package com.seba.grade_iii_iv.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ReactRouterController {
	
	 @RequestMapping(value = {
	            "/",
	            "/slrc",
	            "/slrc/register",
	            "/slrc/Login",
	            "/slrc/register"
	    })
	    public String redirect() {
	        return "forward:/index.html";
	    }

}
