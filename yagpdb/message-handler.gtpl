{{/*
	Handles messages for the leveling system.
	
	See template docs here: https://docs.yagpdb.xyz/reference/templates
	
	Fork of <https://yagpdb-cc.github.io/leveling/message-handler> 
	Original Author: jo3-l <https://github.com/jo3-l>
	Edited by: davcri <https://davcri.github.io>
 */}}

{{ $settings := 0 }} {{/* Instantiate settings at nil value */}}
{{ $roleRewards := sdict "type" "stack" }} {{/* Default role reward settings */}}
{{ with (dbGet 0 "xpSettings") }} {{ $settings = sdict .Value }} {{ end }} {{/* If in db, then we update value */}}
{{ with (dbGet 0 "roleRewards") }} {{ $roleRewards = sdict .Value }} {{ end }} {{/* See above */}}

{{ if $settings }}
	{{ $amtToGive := randInt $settings.min $settings.max }} {{/* Amount of XP to give */}}
	{{ $currentXp := 0 }} {{/* User current XP */}}

	{{ $content := lower .Message.Content }}
	{{ $containsThanks := reFind "grazie" $content }}
	{{ if $containsThanks }}
		{{ $channel := or $settings.channel .Channel.ID }}
		{{ if gt (len .Message.Mentions) 0 }}
			{{ $mentionedUser := index .Message.Mentions 0 }}
			
			{{ if $mentionedUser }}				
				{{ if ne $mentionedUser.ID .User.ID }}
					{{ with (dbGet $mentionedUser.ID "xp") }}
						{{ $currentXp = .Value }}
					{{ end }} {{/* Update XP amount if present */}}
					{{ $currentLvl := roundFloor (mult 0.1 (sqrt $currentXp)) }} {{/* Calculate level */}}
					{{ $newXp := dbIncr $mentionedUser.ID "xp" $amtToGive }} {{/* Increment the xp */}}
					{{ $newLvl := roundFloor (mult 0.1 (sqrt $newXp)) }} {{/* Calculate new level */}}
					{{ if not (.Guild.GetChannel $channel) }} {{ $channel = .Channel.ID }} {{ end }}
					{{ sendMessage $channel (printf "✨ %s ha ricevuto +%d Punti Esperienza Mentore" $mentionedUser.Mention $amtToGive ) }}

					{{ if ne $newLvl $currentLvl }} {{/* If the level changed / user ranked up */}}
						{{ $type := $roleRewards.type }} {{/* Type of role giving (highest / stack) */}}
						{{ $toAdd := or ($roleRewards.Get (json $newLvl)) 0 }} {{/* Try to get the role reward for this level */}}
						
						{{ range $level, $reward := $roleRewards }} {{/* Loop over role rewards */}}
							{{- if and (ge (toInt $newLvl) (toInt $level)) (not (targetHasRoleID $mentionedUser.ID $reward)) (eq $type "stack") (ne $level "type") }} {{- giveRoleID $mentionedUser.ID $reward }}
							{{- else if and (targetHasRoleID $mentionedUser.ID $reward) (eq $type "highest") $toAdd }} {{- takeRoleID $mentionedUser.ID $reward }} {{- end -}}
						{{ end }}
						{{ if $toAdd }} {{ giveRoleID $mentionedUser.ID $toAdd }} {{ end }}
						{{ $embed := cembed 
							"title" "❯ Level up!"
							"thumbnail" (sdict "url" "https://webstockreview.net/images/emoji-clipart-celebration-4.png")
							"description" (printf "Congratulazioni **%s**! Hai raggiunto il livello %d !" $mentionedUser.Mention (toInt $newLvl))
							"color" 14232643
						}}
						{{ if $settings.announcements }}
							{{ sendMessage $channel (complexMessage "content" "" "embed" $embed) }} {{/* Send levelup notification */}}
						{{ end }}
					{{ end }}
				{{ else }}
					{{ sendMessage $channel "Non puoi ringraziare te stesso!" }}
				{{ end }}
			{{ end }}
		{{ else }}
			{{/* Do this only once in a while? */}}
			{{/* {{ sendMessage $channel "Per favore ricorda di taggare l'utente quando ringrazi, così da fargli guadagnare punti esperienza!"}}  */}}
		{{ end }}
	{{ end }}
{{ end }}
