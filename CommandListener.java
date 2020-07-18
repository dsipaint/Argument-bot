import java.awt.Color;
import java.util.LinkedList;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class CommandListener extends ListenerAdapter
{
	private LinkedList<Discussion> discussions;
	static final String[] SERVERCMD = {
			"**" + Main.PREFIX + "debate @ping_user:** Sends a debate request to the pinged user, accepts a request from this user if a request has already been received in this server.",
			"**" + Main.PREFIX + "pass:** Passes talking perms over to the other user in the active debate you are in for this server.",
			"**" + Main.PREFIX + "leavedebate:** Leaves the active debate you are in, in this server.", 
			"**" + Main.PREFIX + "leaveall:** Leaves all debates you are in across all servers.", 
			"**" + Main.PREFIX + "cancelallrequests:** Cancels all debate requests in every server.", 
			"**" + Main.PREFIX + "cancelrequestsserver:** Cancels all debate requests in this server.",
			"**" + Main.PREFIX + "cancelrequest @Ping_user:** Cancels a debate request in the current server by the pinged user.", 
			"**" + Main.PREFIX + "listdebates:** Lists all debates and requests you have.", 
			"**" + Main.PREFIX + "debatehelp:** Displays this message."};
	static final String[] DMCMD = {
			"**" + Main.PREFIX + "leavedebate server:** Leaves the debate you are in in a particular server, if you are in one.", 
			"**" + Main.PREFIX + "leaveall:** Leave all debates you are currently in across all servers.", 
			"**" + Main.PREFIX + "cancelallrequests:** Cancels all debate requests in every server.", 
			"**" + Main.PREFIX + "cancelrequestsserver server:** Cancels all debate requests in a particular server.",
			"**" + Main.PREFIX + "debatehelp:** Displays this message."};
	/*
	 * TODO:
	 * 	Add a message if a request has already been sent
	 */
	
	public CommandListener()
	{
		discussions = new LinkedList<Discussion>();
	}
	
	public void onGuildMessageReceived(GuildMessageReceivedEvent e)
	{
		String msg = e.getMessage().getContentRaw();
		String[] args = msg.split(" ");
		String userID = e.getAuthor().getId();
		
		//>debate
		if(args[0].equalsIgnoreCase(Main.PREFIX + "debate"))
		{
			for(Discussion d : discussions)
			{
				//if member is in a discussion or has a request in this guild
				if((d.getMember1().getId().equals(userID) || d.getMember2().getId().equals(userID)) && d.getGuild().getId().equals(e.getGuild().getId()))
				{
					//is in an active debate
					if(d.getStatus() == Discussion.Status.ACTIVE)
					{
						e.getChannel().sendMessage("You are already in a discussion in this server!").queue();
						return;
					}
				}
			}
			
			//sent a request
			if(e.getMessage().getMentionedMembers().size() > 0)
			{
				String id = e.getMessage().getMentionedMembers().get(0).getId();
				if(id.equals(userID))
				{
					e.getChannel().sendMessage("You can't have a debate with yourself!").queue();
					return;
				}
				
				for(Discussion d : discussions)
				{
					//check if a request exists between these users in this server-if so, if the requested user is not in a debate, accept
					if(d.getStatus() == Discussion.Status.PENDING)
					{
						if((d.getMember1().getId().equals(userID) && d.getMember2().getId().equals(id)) || (d.getMember1().getId().equals(id) && d.getMember2().getId().equals(userID)))
						{
							//check all debates
							for(Discussion d2 : discussions)
							{
								//check requested user is not in an active debate
								if(d2.getStatus() == Discussion.Status.ACTIVE)
								{
									e.getChannel().sendMessage("That user is already in an active discussion").queue();
									return;
								}
							}
							
							//can only accept a request not sent by you
							if(d.getMember1().getId().equals(id) && d.getMember2().getId().equals(userID))
							{
								d.promoteStatus();
								e.getChannel().sendMessage(e.getMessage().getMentionedMembers().get(0).getAsMention()
										+ "- " + e.getMember().getAsMention() + " accepted your debate request! "
												+ "Use \"" + Main.PREFIX + "leavedebate\" to leave this debate.").queue();
								
								return;
							}
							
							//user is trying to send a request to someone they've already sent a request to
							e.getChannel().sendMessage("You have already sent a request to this user.").queue();

							return;
						}
					}
				}
				
				//if no request was there, make one
				//member1 must be the person who sent the request
				discussions.add(new Discussion(e.getMember(), e.getMessage().getMentionedMembers().get(0)));
				e.getChannel().sendMessage(e.getGuild().getMemberById(id).getAsMention()
						+ "- " + e.getMember().getAsMention() + " wishes to debate with you! Use \""
						+ Main.PREFIX + "debate " + e.getMember().getAsMention() + "\" in this server to accept this request, "
								+ "or use \"" + Main.PREFIX + "cancelrequest " + e.getMember().getAsMention() + "\" to cancel this request").queue();
				
				return;
			}
			else
			{
				e.getChannel().sendMessage("You must specify a user to use this command").queue();
				return;
			}
		}
		
		//>leavedebate
		if(args[0].equalsIgnoreCase(Main.PREFIX + "leavedebate"))
		{
			for(Discussion d : discussions)
			{
				//if member is in a discussion in this guild
				if((d.getMember1().getId().equals(userID) || d.getMember2().getId().equals(userID)) && d.getGuild().getId().equals(e.getGuild().getId()))
				{
					if(d.getStatus() == Discussion.Status.ACTIVE)
					{
						discussions.remove(d);
						e.getChannel().sendMessage("You left a debate with " + d.getOtherMember(e.getMember()).getAsMention()).queue();
						return;
					}
				}
			}
			
			e.getChannel().sendMessage("You are not in any active debates in this server").queue();
			return;
		}
		
		//>leaveall
		if(args[0].equalsIgnoreCase(Main.PREFIX + "leaveall"))
		{
			for(Discussion d : discussions)
			{
				if(d.getStatus() == Discussion.Status.ACTIVE)
				{
					if(d.getMember1().getId().equals(userID) || d.getMember2().getId().equals(userID))
					{
						d.getOtherMember(e.getMember()).getUser().openPrivateChannel().queue((channel) ->
						{
							channel.sendMessage("You left a debate with " + e.getAuthor().getName() + " in "
									+ d.getGuild().getName()).queue();
						});
						discussions.remove(d);
					}
				}
			}
			
			e.getChannel().sendMessage("Left all active debates.").queue();
		}
		
		//>cancelallrequests
		if(args[0].equalsIgnoreCase(Main.PREFIX + "cancelallrequests"))
		{
			for(Discussion d : discussions)
			{
				if(d.getStatus() == Discussion.Status.PENDING)
				{
					if(d.getMember1().getId().equals(userID) || d.getMember2().getId().equals(userID))
					{
						d.getOtherMember(e.getMember()).getUser().openPrivateChannel().queue((channel) ->
						{
							channel.sendMessage("Your debate request with " + e.getAuthor().getName()
									+ " in " + d.getGuild().getName() + " has been cancelled.").queue();
						});
						discussions.remove(d);
					}
				}
			}
			
			e.getChannel().sendMessage("Removed all debate requests.").queue();
		}
		
		//>cancelrequestsserver
		if(args[0].equalsIgnoreCase(Main.PREFIX + "cancelrequestsserver"))
		{
			for(Discussion d : discussions)
			{
				if(d.getStatus() == Discussion.Status.PENDING)
				{
					if((d.getMember1().getId().equals(userID) || d.getMember2().getId().equals(userID)) && d.getGuild().getId().equals(e.getGuild().getId()))
					{
						e.getChannel().sendMessage("Your request with " + d.getOtherMember(e.getMember()).getEffectiveName()
								+ " has been cancelled.").queue();
						discussions.remove(d);
					}
				}
			}
			
			e.getChannel().sendMessage("Removed all debate requests.").queue();
		}
		
		//>cancelrequest
		if(args[0].equalsIgnoreCase(Main.PREFIX + "cancelrequest"))
		{
			if(e.getMessage().getMentionedMembers().size() > 0)
			{
				String id = e.getMessage().getMentionedUsers().get(0).getId();
				for(Discussion d : discussions)
				{
					if(((d.getMember1().getId().equals(userID) && d.getMember2().getId().equals(id)) || (d.getMember1().getId().equals(id) && d.getMember2().getId().equals(userID)) && d.getGuild().getId().equals(e.getGuild().getId())))
					{
						if(d.getStatus() == Discussion.Status.PENDING)
						{
							e.getChannel().sendMessage("Request was cancelled with " + d.getOtherMember(e.getMember()).getAsMention()).queue();
							discussions.remove(d);
						}
					}
						
				}
			}
			else
			{
				e.getChannel().sendMessage("You must specify a user to use this command.").queue();
				return;
			}
		}
		
		//>listdebates
		if(args[0].equalsIgnoreCase(Main.PREFIX + "listdebates"))
		{
			String reply = "";
			
			for(Discussion d : discussions)
			{
				if((d.getMember1().getId().equals(userID) || d.getMember2().getId().equals(userID)) && d.getGuild().getId().equals(e.getGuild().getId()))
				{
					if(d.getStatus() == Discussion.Status.PENDING)
						reply += "\n" + d.getOtherMember(e.getMember()).getEffectiveName() + " (Request), Server: " + d.getGuild().getName();
					else
						reply += "\n" + d.getOtherMember(e.getMember()).getEffectiveName() + " (In session), Server: " + d.getGuild().getName();
				}
			}
			
			if(reply.isEmpty())
				e.getChannel().sendMessage("You have no debates or requests").queue();
			else
				e.getChannel().sendMessage(reply).queue();
			
			return;
		}
		
		//>stopdebates
		if(args[0].equalsIgnoreCase(Main.PREFIX + "stopdebates") && userID.equals("475859944101380106"))
		{
			e.getChannel().sendMessage("Shutting down...").queue();
			Main.jda.shutdown();
			System.exit(0);
		}
		
		//>debatehelp
		if(args[0].equalsIgnoreCase(Main.PREFIX + "debatehelp"))
		{
			EmbedBuilder eb = new EmbedBuilder();
			eb.setTitle("**Server Commands:**");
			eb.setColor(new Color(48, 122, 207));
			String desc = "";
			
			for(int i = 0; i < SERVERCMD.length; i++)
			{
				desc += "\n\n" + SERVERCMD[i];
				
				if((i+1)%10 == 0)
				{
					eb.setDescription(desc);
					e.getChannel().sendMessage(eb.build()).queue();
					desc = "";
				}
			}
			
			if(!desc.isEmpty())
			{
				eb.setDescription(desc);
				e.getChannel().sendMessage(eb.build()).queue();
				desc = "";
			}
			
			eb.setTitle("**DM Commands:**");
			
			for(int i = 0; i < DMCMD.length; i++)
			{
				desc += "\n\n" + DMCMD[i];
				
				if((i+1)%10 == 0)
				{
					eb.setDescription(desc);
					e.getChannel().sendMessage(eb.build()).queue();
					desc = "";
				}
			}
			
			if(!desc.isEmpty())
			{
				eb.setDescription(desc);
				e.getChannel().sendMessage(eb.build()).queue();
			}
			
			return;
		}
		
		//>pass
		if(args[0].equalsIgnoreCase(Main.PREFIX + "pass"))
		{
			for(Discussion d : discussions)
			{
				//find the debate in question
				if((d.getMember1().getId().equals(userID) || d.getMember2().getId().equals(userID)) && d.getStatus() == Discussion.Status.ACTIVE && d.getGuild().getId().equals(e.getGuild().getId()))
				{
					//if they are allowed to pass, because they are the speaker
					if(d.getSpeaker().getId().equals(userID))
					{
						d.pass();
						e.getChannel().sendMessage("Talking perms were passed to " + d.getSpeaker().getAsMention() + "!").queue();
					}
				}
			}
		}
	}
	
	public void onPrivateMessageReceived(PrivateMessageReceivedEvent e)
	{
		String msg = e.getMessage().getContentRaw();
		String[] args = msg.split(" ");
		String userID = e.getAuthor().getId();
		
		//>leavedebate server
		if(args[0].equalsIgnoreCase(Main.PREFIX + "leavedebate"))
		{
			String srv = "";
			for(int i = 1; i < args.length; i++)
				srv += " " + args[i];
			
			if(!srv.isEmpty())
			{
				for(Discussion d : discussions)
				{
					if(d.getGuild().getName().equalsIgnoreCase(srv) && d.getStatus() == Discussion.Status.ACTIVE)
					{
						if(d.getMember1().getId().equals(userID) || d.getMember2().getId().equals(userID))
						{
							e.getChannel().sendMessage("Your debate with " + d.getOtherMember(d.getGuild().getMember(e.getAuthor())).getUser().getName()
									+ " ended in " + d.getGuild().getName()).queue();
							
							d.getOtherMember(d.getGuild().getMember(e.getAuthor())).getUser().openPrivateChannel().queue((channel) ->
							{
								channel.sendMessage("Your debate with " + e.getAuthor().getName() + " ended in "
										+ d.getGuild().getName()).queue();
							});
							
							discussions.remove(d);
							
							return;
						}
					}
				}
			}
			
			return;
		}
		
		//>leaveall
		if(args[0].equalsIgnoreCase(Main.PREFIX + "leaveall"))
		{
			for(Discussion d : discussions)
			{
				if(d.getStatus() == Discussion.Status.ACTIVE)
				{
					if(d.getMember1().getId().equals(userID) || d.getMember2().getId().equals(userID))
					{
						e.getChannel().sendMessage("You left a debate with " + d.getOtherMember(d.getGuild().getMember(e.getAuthor())).getUser().getName()
								+ " in " + d.getGuild().getName()).queue();
						
						d.getOtherMember(d.getGuild().getMember(e.getAuthor())).getUser().openPrivateChannel().queue((channel) ->
						{
							channel.sendMessage("You left a debate with " + e.getAuthor().getName() + " in "
									+ d.getGuild().getName()).queue();
						});
						discussions.remove(d);
					}
				}
			}
			
			e.getChannel().sendMessage("Left all active debates.").queue();
			return;
		}
		
		//>cancelallrequests
		if(args[0].equalsIgnoreCase(Main.PREFIX + "cancelallrequests"))
		{
			for(Discussion d : discussions)
			{
				if(d.getStatus() == Discussion.Status.PENDING)
				{
					if(d.getMember1().getId().equals(e.getAuthor().getId()) || d.getMember2().getId().equals(e.getAuthor().getId()))
					{
						e.getChannel().sendMessage("Your debate request with " + d.getOtherMember(d.getGuild().getMember(e.getAuthor())).getUser().getName()
								+ " was cancelled.").queue();
						
						d.getOtherMember(d.getGuild().getMember(e.getAuthor())).getUser().openPrivateChannel().queue((channel) ->
						{
							channel.sendMessage("Your debate request with " + e.getAuthor().getName() + " was cancelled.").queue();
						});
					}
				}
			}
			
			e.getChannel().sendMessage("Removed all debate requests.").queue();
			return;
		}
		
		//>cancelrequestsserver server
		if(args[0].equalsIgnoreCase(Main.PREFIX + "cancelrequestsserver"))
		{
			String srv = "";
			for(int i = 1; i < args.length; i++)
				srv += " " + args[i];
			
			if(!srv.isEmpty())
			{
				for(Discussion d : discussions)
				{
					if(d.getGuild().getName().equals(srv) && d.getStatus() == Discussion.Status.PENDING)
					{
						if(d.getMember1().getId().equals(userID) || d.getMember2().getId().equals(userID))
						{
							e.getChannel().sendMessage("Your request with " + d.getOtherMember(d.getGuild().getMember(e.getAuthor())).getUser().getName()
									+ " in " + d.getGuild().getName() + " has been cancelled.").queue();
							d.getOtherMember(d.getGuild().getMember(e.getAuthor())).getUser().openPrivateChannel().queue((channel) ->
							{
								channel.sendMessage("Your request with " + e.getAuthor().getName() + " in "
										+ d.getGuild().getName() + " had been cancelled.").queue();
							});
							discussions.remove(d);
						}
					}
				}
			}
		}
		
		//>debatehelp
		if(args[0].equalsIgnoreCase(Main.PREFIX + "debatehelp"))
		{
			EmbedBuilder eb = new EmbedBuilder();
			eb.setTitle("**Server Commands:**");
			eb.setColor(new Color(48, 122, 207));
			String desc = "";
			
			for(int i = 0; i < SERVERCMD.length; i++)
			{
				desc += "\n\n" + SERVERCMD[i];
				
				if((i+1)%10 == 0)
				{
					eb.setDescription(desc);
					e.getChannel().sendMessage(eb.build()).queue();
					desc = "";
				}
			}
			
			if(!desc.isEmpty())
			{
				eb.setDescription(desc);
				e.getChannel().sendMessage(eb.build()).queue();
				desc = "";
			}
			
			eb.setTitle("**DM Commands:**");
			
			for(int i = 0; i < DMCMD.length; i++)
			{
				desc += "\n\n" + DMCMD[i];
				
				if((i+1)%10 == 0)
				{
					eb.setDescription(desc);
					e.getChannel().sendMessage(eb.build()).queue();
					desc = "";
				}
			}
			
			if(!desc.isEmpty())
			{
				eb.setDescription(desc);
				e.getChannel().sendMessage(eb.build()).queue();
			}
		}
	}
	
	public void onGuildMemberLeave(GuildMemberLeaveEvent e)
	{
		String userID = e.getUser().getId();
		
		if(userID.equals(e.getJDA().getSelfUser().getId()))
		{
			for(Discussion d : discussions)
			{
				if(d.getGuild().getId().equals(e.getGuild().getId()))
					discussions.remove(d);
			}
			
			return;
		}
		
		for(Discussion d : discussions)
		{
			if((d.getMember1().getId().equals(userID) || d.getMember2().getId().equals(userID)) && d.getGuild().getId().equals(e.getGuild().getId()))
				discussions.remove(d);
		}
	}
}
