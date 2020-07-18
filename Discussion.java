import java.awt.Color;
import java.util.EnumSet;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

public class Discussion
{
	static enum Status{
		ACTIVE, PENDING
	}
	
	private Status status;
	private Member member1, member2, speaker;
	
	public Discussion(Member member1, Member member2)
	{
		this.member1 = member1;
		this.member2 = member2;
		speaker = null;
		this.status = Status.PENDING;
	}
	
	public void promoteStatus()
	{
		status = Status.ACTIVE;
		speaker = member1; //person who started the debate should open
		swapGuildMutedRole();
	}
	
	public void pass()
	{
		if(speaker != null)
		{
			if(speaker.getId().equals(member1.getId()))
				speaker = member2;
			else
				speaker = member1;
			
			swapGuildMutedRole();
		}
	}
	
	public Status getStatus()
	{
		return status;
	}
	
	public Member getOtherMember(Member m)
	{
		if(m.getId().equals(member1.getId()))
			return member2;
		else if(m.getId().equals(member2.getId()))
			return member1;
		else
			return null;
	}

	public Member getMember1()
	{
		return member1;
	}

	public Member getMember2()
	{
		return member2;
	}	
	
	public Guild getGuild()
	{
		return member1.getGuild();
	}
	
	public Member getSpeaker()
	{
		return speaker;
	}
	
	private void swapGuildMutedRole()
	{
		if(speaker == null)
			return;
		
		for(Role r : Main.mutedroles)
		{
			if(r == null)
				continue;
			
			if(r.getGuild().getId().equals(getGuild().getId()))
			{
				getGuild().addRoleToMember(getOtherMember(speaker), r).queue();
				getGuild().removeRoleFromMember(speaker, r).queue();
				return;
			}
		}
		
		getGuild().createRole().queue((r) ->
		{
			r.getManager().setName("Debate-bot muted role")
			.setColor(new Color(52,152,235))
			.revokePermissions(EnumSet.of(Permission.MESSAGE_WRITE, Permission.VOICE_SPEAK))
			.queue();
			
			getGuild().addRoleToMember(getOtherMember(speaker), r).queue();
			getGuild().removeRoleFromMember(speaker, r).queue();
			
			Main.mutedroles.add(r);
		});
	}
}
