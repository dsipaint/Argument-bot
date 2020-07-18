import java.util.LinkedList;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Role;

public class Main
{
	static JDA jda;
	static final String PREFIX = ">";
	static LinkedList<Role> mutedroles;
	
	public static void main(String[] args)
	{
		try
		{
			jda = new JDABuilder(AccountType.BOT).setToken("").build();
		}
		catch (LoginException e)
		{
			e.printStackTrace();
		}
		
		try
		{
			jda.awaitReady();
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		
		mutedroles = new LinkedList<Role>();
		
		jda.getPresence().setActivity(Activity.playing("Use \"" + Main.PREFIX + "debatehelp\" for help"));
		jda.addEventListener(new CommandListener());
	}
}
